import collections.abc
import warnings
import inspect
from typing import Callable, TypeVar, Union
#from collections.abc import Callable  # For Python 3.9+, else use typing.Callable

from rppmcontroller.behavior.CycleStepResult import CycleStepResult

from rppmcontroller.machine.Runner import Runner

F = TypeVar("F", bound=Callable[..., Union[Runner, Callable[[], CycleStepResult]]])  # Function with any args returning  either a Runner or a function returning a CycleStepResult


def protocol_command_function(description: str = ""):
    """
    This annotation indicates that the annotated function is intended to be used as a protocol command.

    Requirements:
    - Must return a CycleStepResult (or subclass like Runner)
    - Function name must end with "_Command" (may be replaced by introspection later)
    """
    def decorator(func: F) -> F:
        # === Name enforcement ===
        suffix: str = "_Command"
        func_name: str = func.__name__
        if not func_name.endswith(suffix):
            raise ValueError(f"Function name '{func_name}' must end with '{suffix}'.")

        prefix: str = func_name[:-len(suffix)]
        if not prefix.islower():
            raise ValueError(f"Function name prefix '{prefix}' must be all lowercase before '{suffix}'.")
        # === Signature enforcement ===
        sig = inspect.signature(func)
        
        # enforce return annotation
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


                def is_valid_cycle_result_type(tp: object) -> bool:
                    if inspect.isclass(tp) and issubclass(tp, Runner):
                        return True
                    origin = getattr(tp, '__origin__', None)
                    args = getattr(tp, '__args__', ())
                    if origin in (Callable, collections.abc.Callable):
                        # Handle both ([], CycleStepResult) and just (CycleStepResult,) due to 3.7 issues
                        if len(args) == 1:
                            # Workaround for broken Python 3.7 case: Callable[[], T] sometimes becomes Callable[T]
                            return inspect.isclass(args[0]) and issubclass(args[0], CycleStepResult)
                        elif len(args) == 2:
                            return (
                                args[0] == [] and
                                inspect.isclass(args[1]) and issubclass(args[1], CycleStepResult)
                            )
                    return False


                origin = getattr(resolved, '__origin__', None)
                if origin is Union:
                    if not all(is_valid_cycle_result_type(arg) for arg in getattr(resolved, '__args__', ())):
                        raise TypeError(
                            f"{func.__name__} must return Runner or Callable[[], CycleStepResult], "
                            f"or a Union of those. Found: {resolved}"
                        )
                elif not is_valid_cycle_result_type(resolved):
                    raise TypeError(
                        f"{func.__name__} must return Runner or Callable[[], CycleStepResult] (got {return_type})"
                    )

            except Exception as e:
                raise TypeError(f"Could not verify return type for {func.__name__}: {e}")


        # === Attach metadata ===
        func.is_protocol_command = True                         # type: ignore
        func.protocol_command_description = description         # type: ignore
        return func
    return decorator