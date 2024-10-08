import numpy as np



def convert_to_serializable(obj):
    """Recursively converts numpy types to native Python types for serialization.

    Parameters:
        obj: Object to be converted.

    Returns:
        Object: Converted object with numpy types replaced by Python types.
    """
    if isinstance(obj, np.integer):
        return int(obj)
    if isinstance(obj, np.floating):
        return float(obj)
    if isinstance(obj, np.ndarray):
        return obj.tolist()
    if isinstance(obj, dict):
        return {k: convert_to_serializable(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [convert_to_serializable(v) for v in obj]
    return obj
