import numpy as np
from dataclasses import dataclass
from MatrixPythonAPI import *

# common util functions for scanning / lithography

@dataclass
class ScanCondition:
    vgap: float
    iset: float
    tipspeed: float


# common litho conditions
defaultPrecisionLitho = ScanCondition(4.0, 3.0, 80.0)
slowPrecisionLitho = ScanCondition(4.0, 3.0, 20.0)
medianCurrentLitho = ScanCondition(4.0, 10.0, 80.0)
highCurrentLitho = ScanCondition(4.0, 50.0, 80.0)
defaultFieldEmission = ScanCondition(6.0, 2.0, 200.0)


# load common litho conditions
def loadLithoCondition(condition: ScanCondition) -> None:
    setLithoCondition(condition.vgap, condition.iset, condition.tipspeed)

# set scan condition change V first then I
def setScanCondtionVI(condition: ScanCondition) -> None:
    RampV(condition.vgap)
    setiSet(condition.iset)
    changeTipSpeed(condition.tipspeed)

# set scan condition change I first then V
def setScanCondtionIV(condition: ScanCondition) -> None:
    setiSet(condition.iset)
    RampV(condition.vgap)
    changeTipSpeed(condition.tipspeed)

# get all information of scan window
def getWindowInfo() -> tuple:
    return getWindowSize(), getWindowPosition(), getScanAngle(), getResolution(), getWorldPosition()

# define scan window
def setWindow(wh: tuple, xy: tuple, theta: int, res: (tuple)) -> None:
    setWindowSize(wh)
    wait_ms(2000)
    setWindowPosition(xy)
    wait_ms(2000)
    setWindowAngle(theta)
    wait_ms(1000)
    setResolution(res)

# get tip position in the window coordinates
def getTipPosition() -> np.ndarray:
    _angle = np.deg2rad(getScanAngle())
    _center = getWindowPosition()
    _tipRawPosition = getRawTipPosition()
    _delta = _tipRawPosition - _center
    ey = np.array([np.sin(_angle), np.cos(_angle)])
    ex = np.array([np.cos(_angle), -np.sin(_angle)])
    return np.array([_delta @ ex, _delta @ ey])


# scan partial area within the current scanwindow (slow)

#
#             (r2.x, r2.y) ------------------
#           /
#         /
#       /
#   (r0.x, r0.y) -------------------- (r1.x, r1.y)

def partialScan(r0: np.ndarray, r1: np.ndarray, r2: np.ndarray, pts: int = 11, lns: int = 11) -> tuple:
    Z_data = np.zeros((lns, pts))
    I_data = np.zeros((lns, pts))
    # insure the inputs are np array
    r0 = np.array(r0)
    r1 = np.array(r1)
    r2 = np.array(r2)
    dy = (r2 - r0) / (lns - 1)

    for l in range(lns):
        Z_trace, I_trace = getLineProfile(r0=r0+l*dy, r1=r1+l*dy, pts=pts)
        Z_data[lns - 1 - l, :] = Z_trace
        I_data[lns - 1 - l, :] = I_trace
    return Z_data, I_data


# get new scan image (wrapper getNewScanImage)
def getNewImage(startline: int = 0, endline: int = -1) -> tuple:
    res = getResolution().tolist()
    if endline == -1:
       endline = res[1] - 1
    new_img = getNewScanImage(startline, endline)
    new_img = np.array(new_img).reshape(endline-startline+1, res[0])
    return res, new_img


# continous lithography from point lists (origin at window center) 
def lithoPointList(lithopath: list, lithoCondition: ScanCondition):
    # moving to the first point
    moveTip(lithopath[0])
    # save the current scan setting and switch to litho setting
    savedCondition = ScanCondition(getvGap(), getiSet(), getTipSpeed())
    setScanCondtionVI(lithoCondition)
    # move according to the point list
    for point in lithopath:
        moveTip(point)
    # pop the previous scan setting
    setScanCondtionIV(savedCondition)

# controlled operation at multipled points
# example: controlledOperation([(0,0),(50, 50)], op=print, "OK")
def controlledOperation(coords: list, op, *args):
    for point in coords:
        moveTip(point)
        op(*args)


# continous lithography from point lists (origin at current tip position)
def d_lithoPointList(lithopath: list, lithoCondition: ScanCondition):
    _tippos = getTipPosition()
    _targetpos = (_tippos + np.array(lithopath[0])).tolist()
    moveTip(_targetpos)
    savedCondition = ScanCondition(getvGap(), getiSet(), getTipSpeed())
    setScanCondtionVI(lithoCondition)
    for point in lithopath:
        _targetpos = (_tippos + np.array(point)).tolist()
        moveTip(_targetpos)
    setScanCondtionIV(savedCondition)