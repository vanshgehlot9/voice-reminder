import os
import sys
import importlib.util

# Ensure backend directory is in sys.path
backend_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "backend", "lightweight-backend")
if backend_dir not in sys.path:
    sys.path.insert(0, backend_dir)

# Load the server module from backend/lightweight-backend/server.py avoiding circular import
server_file = os.path.join(backend_dir, "server.py")
spec = importlib.util.spec_from_file_location("backend_server", server_file)
backend_server = importlib.util.module_from_spec(spec)
sys.modules["backend_server"] = backend_server
spec.loader.exec_module(backend_server)

app = backend_server.app

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 9000))
    uvicorn.run(app, host="0.0.0.0", port=port)
