import socket
import json
import numpy as np
import time
import select

from typing import Tuple
from pydantic import (
    BaseModel,
    PositiveInt,
    PositiveFloat,
)

class float2(BaseModel):
    values: Tuple[float, float]

class pfloat2(BaseModel):
    values: Tuple[PositiveFloat, PositiveFloat]

class pint2(BaseModel):
    values: Tuple[PositiveInt, PositiveInt]

class pint(BaseModel):
    values: PositiveInt

class pfloat(BaseModel):
    values: PositiveFloat




# package types
pkg = {"QUERY": 0,
       "ACTION": 1,
       "START": 2}

# operation types
op = {"WAIT": 0,
      "MVTIP": 1,
      "CVGAP": 2,
      "CISET": 3,
      "STARTSCAN": 4,
      "STOPSCAN": 5,
      "CSPEED": 6,
      "CWINSIZE": 7,
      "CWINPOS": 8,
      "CANGLE": 9,
      "VRAMP": 10,
      "STARTLITHO": 11,
      "STOPLITHO": 12,
      "LITHOIV": 13,
      "FCLVRAMP": 14,
      "ZPULSE": 15,
      "VPULSE": 16,
      "SETFL": 17,
      "SETDZ": 18,
      "CRESOLUTION": 19,
      "FREETIP": 20}

# query types
qry = {"WINSIZE": 0,
       "WINPOS": 1,
       "TIPPOS": 2,
       "VGAP": 3,
       "ISET": 4,
       "IMAGE": 5,
       "MEASLINE": 6,
       "NEWIMAGE": 7,
       "GDSNAME": 8,
       "ANGLE": 9,
       "WORLDPOS": 10,
       "RESOLUTION": 11,
       "TIPSPEED": 12}

# default ip address and port number
def_host : str = 'localhost'
def_port : int = 12345

#
seq = 0

# setup up socket connection with Monitor
def connect_to_Monitor() -> socket:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((def_host, def_port))
        #print(f"Connected to server {def_host}:{def_port}")
        return s
        
    except Exception as e:
        print(f"An error occurred: {e}")
        return None

def send_and_receive_json(j: dict, timeout=5) -> dict:
	data = ""
	start_time = time.time()
    
    # connect to the server first
	s = connect_to_Monitor()
    # assemble bytestring and send to server
	try:    
		json_data = json.dumps(j)
		# Send the JSON data followed by a newline character
		s.sendall(json_data.encode('utf-8') + b'\n')
        
		while True:
			ready = select.select([s], [], [], timeout)
			if ready[0]:
				chunk = s.recv(1024).decode("utf-8")
				if not chunk:
					raise ConnectionError("Connection closed by client")
				data += chunk
				#print(data)
				try:
					return json.loads(data)
				except json.JSONDecodeError:
					if time.time() - start_time > timeout:
						raise TimeoutError("Timeout while waiting for complete JSON")
					continue
			else:
				raise TimeoutError("Timeout while waiting for data")
	            
	except Exception as e:
		print(f"An error occurred: {e}")
            
# send all steps as json to Monitor
def send_json_to_server(j: dict, chunk: bool = False):
    # connect to the server first
    s = connect_to_Monitor()
    # assemble bytestring and send to server
    try:    
        json_data = json.dumps(j)
        # Send the JSON data followed by a newline character
        s.sendall(json_data.encode('utf-8') + b'\n')
        #print(f"Sent data: {json_data}")
            
        # Receive the confirmation response from the server
        if chunk is False:
            response = s.recv(1024)
        else:
            response = b""
            while True:
                #segment = s.recv(4096)
                segment = s.recv(4096)
                if not segment:
                    break
                response += segment
        #print(f"Received data: {response.decode('utf-8')}")
        return response
    except Exception as e:
        print(f"An error occurred: {e}")

# wait (ms)
def wait_ms(t: int):
    pint(values=t)
    global seq
    _data = {"t": t}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["WAIT"], "data": _data}
    seq += 1
    send_json_to_server(_json)
    #print(f"    Waiting for {t}ms...")


# movetip (nm, nm)
def moveTip(targetPosition: tuple):
    float2(values=targetPosition)
    global seq
    _data = {"tipx": targetPosition[0], "tipy": targetPosition[1]}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["MVTIP"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# Change VGAP (V)
def setvGap(v: float):
    global seq
    _data = {"vgap": v}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["CVGAP"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# Change ISET (nA)
def setiSet(i: float):
    pfloat(values=i)
    global seq
    _data = {"iset": i}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["CISET"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# start scanning
def startScan():
    global seq
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["STARTSCAN"]}
    seq += 1
    send_json_to_server(_json)

# stop scanning (todo: find way to detect tip settle)
def stopScan():
    global seq
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["STOPSCAN"]}
    seq += 1
    send_json_to_server(_json)

# change tip speed (nm/s)
def changeTipSpeed(tipspeed: float):
    pfloat(values=tipspeed)
    global seq
    _data = {"tipspeed": tipspeed}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["CSPEED"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# change scan window size (nm,nm)
def setWindowSize(sz: tuple):
    pfloat2(values=sz)
    global seq
    _data = {"w": sz[0], "h": sz[1]}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["CWINSIZE"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# change scan window position/offset (nm,nm)
def setWindowPosition(pos: tuple):
    float2(values=pos)
    global seq
    _data = {"x": pos[0], "y": pos[1]}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["CWINPOS"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# change scan window angle (degree)
def setWindowAngle(theta: int):
    global seq
    _data = {"theta": theta }
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["CANGLE"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# vramp (V)
def RampV(v: float):
    global seq
    _data = {"vramp": v}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["VRAMP"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# set litho condition
def setLithoCondition(v: float, i: float, tipspeed: float):
    global seq
    _data = {"vgap": v, "iset": i, "tipspeed": tipspeed}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["LITHOIV"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# do FCL (vRamp)
def FCL_RampV():
    global seq
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["FCLVRAMP"]}
    seq += 1
    send_json_to_server(_json)


# do Z pulse
def zPulse(dz: float):
    global seq
    _data = {"dz": dz}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["ZPULSE"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# do V pulse
def vPulse(v: float):
    global seq
    _data = {"v": v}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["VPULSE"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# change feedback state
def setFeedbackLoop(state: bool):
    global seq
    _data = {"state": state}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["SETFL"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# change Z offset (nm) when feedback is disabled. Pos = further, Neg = closer
def setZOffset(dz: float):
    global seq
    _data = {"dz": dz}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["SETDZ"], "data": _data}
    seq += 1
    send_json_to_server(_json)

# set scan resolution:  (points, lines) 
def setResolution(res: tuple):
    pint2(values=res)
    global seq
    _data = {"points": res[0], "lines": res[1]}
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["CRESOLUTION"], "data": _data}
    seq += 1
    send_json_to_server(_json)


# disable tip return & execution
def disableTipReturn():
    global seq
    _json = {"type": pkg["ACTION"], "seq": seq, "op": op["FREETIP"]}
    seq += 1
    send_json_to_server(_json)
####################################################################################################################

# get scan window size
def getWindowSize() -> np.ndarray:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["WINSIZE"]}
    seq += 1
    result = json.loads(send_json_to_server(_json).decode('utf-8'))
    return np.array([result['w'], result['h']])

# get scan window position
def getWindowPosition() -> np.ndarray:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["WINPOS"]}
    seq += 1
    result = json.loads(send_json_to_server(_json).decode('utf-8'))
    return np.array([result['x'], result['y']])

# get tip position in the sample space coordinates
def getRawTipPosition() -> np.ndarray:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["TIPPOS"]}
    seq += 1    
    result = json.loads(send_json_to_server(_json).decode('utf-8'))
    return np.array([result['x'], result['y']])

# get vGap
def getvGap() -> float:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["VGAP"]}
    seq += 1
    result = json.loads(send_json_to_server(_json).decode('utf-8'))
    return result['vgap']

# get iSet
def getiSet() -> float:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["ISET"]}
    seq += 1
    result = json.loads(send_json_to_server(_json).decode('utf-8'))
    return result['iset']

# get tipspeed
def getTipSpeed() -> float:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["TIPSPEED"]}
    seq += 1
    result = json.loads(send_json_to_server(_json).decode('utf-8'))
    return result['tipspeed']

# get image buffer
def getCurrentScanImage() -> list:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["IMAGE"]}
    seq += 1
    result = json.loads(send_json_to_server(_json, chunk=True).decode('utf-8'))
    return result['img']

# get Z/I along a line
def getLineProfile(r0: np.ndarray, r1: np.ndarray, pts:int = 11) -> tuple:
    global seq
    _data = {"xi": r0[0], "yi": r0[1], "xf": r1[0], "yf": r1[1], "pts": pts}
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["MEASLINE"], "data": _data}
    seq += 1
    print("    Waiting for line scan ...")
    result = json.loads(send_json_to_server(_json, chunk=True).decode('utf-8'))
    return result['Z'], result['I']

# get new image
def getNewScanImage(startline: int, endline: int) -> list:
    global seq
    _data = {"li": startline, "lf": endline}
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["NEWIMAGE"], "data":_data}
    seq += 1
    print("    Waiting for new scan image ...")
    result = json.loads(send_json_to_server(_json, chunk=True).decode('utf-8'))
    return result['img']

 # get GDS name
def getGDSName() -> str:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["GDSNAME"]}
    seq += 1
    result = json.loads(send_json_to_server(_json).decode('utf-8'))
    return result['GDS']

 # get scan angle
def getScanAngle() -> int:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["ANGLE"]}
    seq += 1
    result = json.loads(send_json_to_server(_json).decode('utf-8'))
    return result['theta']

 # get scan window position in world coordinate
def getWorldPosition() -> np.ndarray:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["WORLDPOS"]}
    seq += 1
    result = json.loads(send_json_to_server(_json).decode('utf-8'))
    return np.array([result['x'], result['y']])

# get scan resolution:
def getResolution() -> np.ndarray:
    global seq
    _json = {"type": pkg["QUERY"], "seq": seq, "op": qry["RESOLUTION"]}
    
    seq += 1
    
    result = send_and_receive_json(_json)
    
    #returnVal = send_json_to_server(_json, True)
    #print( returnVal.decode('utf-8') )
    
    #result = {"points":201,"lines":201}
    
    #result = json.loads( returnVal.decode('utf-8') )
    return np.array([result['points'], result['lines']])


if __name__ == "__main__":
    # make up data
    # Define the JSON data to be sent
    print("do nothing")