import inspect
from typing import Callable, Any

def describe_callable(cb: Callable[..., Any]) -> str:
    """
    Attempt to return a human-readable description of a callable:
    - If it's a lambda, shows file and line number with source.
    - If it's a named function, shows name and location.
    - Falls back to type info on failure.

    :param cb: The callable to describe.
    :return: A string describing the callable.
    """
    try:
        name: str = getattr(cb, '__name__', type(cb).__name__)

        if name == "<lambda>":
            src_info: str = inspect.getsourcefile(cb) or "<unknown file>"
            src_lines: list[str]
            line_no: int
            src_lines, line_no = inspect.getsourcelines(cb)
            return f"lambda defined at {src_info}:{line_no}:\n  {''.join(src_lines).strip()}"
        else:
            src_info: str = inspect.getsourcefile(cb) or "<unknown file>"
            line_no: int = inspect.getsourcelines(cb)[1]
            return f"function '{name}' defined at {src_info}:{line_no}"
    except Exception as e:
        return f"Could not describe callable: {e}"
