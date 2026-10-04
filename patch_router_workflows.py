import re

file_path = "/Users/vanshgehlot/voicebox-main/backend/routes/__init__.py"

with open(file_path, "r") as f:
    content = f.read()

import_statement = "from . import workflows"
register_statement = "router.include_router(workflows.router)"

if import_statement not in content:
    content = content.replace("from . import workspace", "from . import workspace\n" + import_statement)
    content = content.replace("router.include_router(workspace.router)", "router.include_router(workspace.router)\n    " + register_statement)
    
    with open(file_path, "w") as f:
        f.write(content)
    print("Router patched for workflows.")
else:
    print("Already patched.")
