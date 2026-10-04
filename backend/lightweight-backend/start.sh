#!/bin/bash
cd "$(dirname "$0")"

# Create a virtual environment for the lightweight backend
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

# Activate and install dependencies
source venv/bin/activate
pip install -r requirements.txt

# Free port 9000 if already in use
lsof -ti :9000 | xargs kill -9 2>/dev/null || true

# Start the server on port 9000
echo "Starting Zero-Memory Backend on port 9000..."
python server.py
