import collections.abc
import warnings
import inspect
from typing import Callable, TypeVar, Union
#from collections.abc import Callable  # For Python 3.9+, else use typing.Callable


from rppmcontroller.behavior.CycleStepResult import CycleStepResult

from rppmcontroller.machine.Machine import Machine

F = TypeVar("F", bound=Callable[..., Callable[..., CycleStepResult]])  # Function with any args returning a function returning a CycleStepResult


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

                args = getattr(resolved, '__args__', ())
                origin = getattr(resolved, '__origin__', None)
                if inspect.isclass(resolved) and issubclass(resolved, CycleStepResult):
                    pass
                elif origin in (Callable, collections.abc.Callable):
                    if args and len(args) == 2:
                        param_types, return_type_candidate = args
                        if param_types == [] and inspect.isclass(return_type_candidate) and issubclass(return_type_candidate, CycleStepResult):
                            pass  # ✅ Callable[[], CycleStepResult] — OK
                        else:
                            raise TypeError(
                                f"{func.__name__} has Callable return type, but expected Callable[[], CycleStepResult] (got {args})"
                            )
                    # Workaround for broken Python 3.7 case: Callable[[], T] sometimes becomes Callable[T]
                    elif len(args) == 1:
                        callable_return_type = args[0]
                        if inspect.isclass(callable_return_type) and issubclass(callable_return_type, CycleStepResult):
                            pass  # ✅ Callable[[], CycleStepResult] — OK
                        else:
                            raise TypeError(
                                f"{func.__name__} has Callable return type, but expected Callable[[], CycleStepResult] (got {args})"
                            )
                    else:
                        raise TypeError(
                            f"{func.__name__} has malformed Callable return annotation (got args={args})"
                        )
                else:
                    warnings.warn(
                        f"⚠️ {func.__name__} must return CycleStepResult or Callable[[], CycleStepResult] (got {return_type})"
                    )
                    raise TypeError(
                        f"{func.__name__} must return CycleStepResult or Callable[[], CycleStepResult] (got {return_type})"
                    )
            except Exception as e:
                raise TypeError(f"Could not verify return type for {func.__name__}: {e}")


        # === Attach metadata ===
        func.is_protocol_command = True                         # type: ignore
        func.protocol_command_description = description         # type: ignore
        return func
    return decorator