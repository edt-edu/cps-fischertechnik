import inspect
from typing import get_type_hints

from rppmcontroller.behavior.CycleStepResult import CycleStepResult

def protocol_command_function(description: str = ""):
    """
    This annotation indicates that tha annotated function is mapped to a protocol command
       
    The annotated function must conforms to the following rules:
    - must return a CycleStepResult (or a sub class such as Runner)
    - function name must end with "_Command" for discovery by RevPiPyMachineController.processJson()  
         (may be replaced later by annotation introspection instead)
    """
    def decorator(func):
        # === Signature enforcement ===
        sig = inspect.signature(func)
        # params = list(sig.parameters.values())

        # if len(params) not in (0, 1):
        #     raise TypeError(f"{func.__name__} must have 0 or 1 parameters (found {len(params)}).")

        # if len(params) == 1:
        #     param = params[0]
        #     if param.name != "runner":
        #         raise TypeError(f"{func.__name__} must name its sole parameter 'runner'.")
        
        # enforce return annotation
        return_type = sig.return_annotation
        if return_type is inspect.Signature.empty:
            print(f"⚠️ Warning: {func.__name__} has no return type annotation.")
        else:
            # Handle ForwardRef or typing issues by resolving fully
            try:
                # Optional: resolve from annotations if it's a string (e.g. 'Runner')
                resolved = return_type
                if isinstance(return_type, str):
                    resolved = eval(return_type, func.__globals__)

                if not inspect.isclass(resolved) or not issubclass(resolved, CycleStepResult):
                    raise TypeError(
                        f"{func.__name__} must return CycleStepResult or subclass (got {return_type})"
                    )
            except Exception as e:
                raise TypeError(f"Could not verify return type for {func.__name__}: {e}")

        # === Attach metadata ===
        func.is_protocol_command = True
        func.protocol_command_description = description
        return func
    return decorator