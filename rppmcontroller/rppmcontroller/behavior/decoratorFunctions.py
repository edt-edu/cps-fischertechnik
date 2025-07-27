import collections.abc
import warnings
import inspect
from typing import Callable, TypeVar, Union
#from collections.abc import Callable  # For Python 3.9+, else use typing.Callable

from rppmcontroller.behavior.CycleStepResult import CycleStepResult

from rppmcontroller.machine.Runner import Runner

F = TypeVar("F", bound=Callable[...,CycleStepResult])  # Function with any args returning a CycleStepResult


def cycle_step_function():
    """
    This annotation indicates that the annotated function is intended to be run in one cycle 
    and be repeatedly called until its result is not  MUST_CONTINUE

    A CycleStep function may declare arguments. For this, the function must be used via a lambda that will set the arguments values.

    Requirements:
    - Must return a CycleStepResult
    - Must be called again on next cycle if the result is MUST_CONTINUE
    """
    def decorator(func: F) -> F:
        # === Signature enforcement ===
        sig = inspect.signature(func)
        
        # === enforce return annotation
        return_type = sig.return_annotation
        if return_type is inspect.Signature.empty:
            warnings.warn(f"⚠️ {func.__name__} has no return type annotation.")
        else:
            # Handle ForwardRef or typing issues by resolving fully
            try:
                # Optional: resolve from annotations if it's a string (e.g. 'Runner')
                resolved = return_type
                if isinstance(return_type, str):
                    resolved = eval(return_type, func.__globals__)

                if not inspect.isclass(resolved) or not issubclass(resolved, CycleStepResult):
                    raise TypeError(
                        f"{func.__name__} must return CycleStepResult (got {return_type})"
                    )                
            except Exception as e:
                raise TypeError(f"Could not verify return type for {func.__name__}: {e}")

        # === Attach metadata ===
        func.is_cycle_step = True                         # type: ignore
        return func
    return decorator