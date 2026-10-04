import sys
import os

# Add backend to python path if we are running this script
backend_dir = "/Users/vanshgehlot/voicebox-main"
if backend_dir not in sys.path:
    sys.path.insert(0, backend_dir)

from backend.database import session
from backend.database.models import User
from backend.security import get_password_hash

session.init_db()
db = session.SessionLocal()

# Check if test user exists
user = db.query(User).filter(User.email == "test@voicebox.com").first()
if not user:
    # Create test user
    hashed_password = get_password_hash("password123")
    user = User(email="test@voicebox.com", hashed_password=hashed_password)
    db.add(user)
    db.commit()
    print("Created test user: test@voicebox.com / password123")
else:
    print("Test user already exists: test@voicebox.com / password123")
