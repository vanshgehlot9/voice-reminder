import re

file_path = "/Users/vanshgehlot/voicebox-main/backend/database/models.py"

with open(file_path, "r") as f:
    content = f.read()

workflow_models = """

class Workflow(Base):
    \"\"\"Agentic Workflow execution instance.\"\"\"

    __tablename__ = "workflows"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id"), nullable=True)
    goal = Column(String, nullable=False)
    status = Column(String, default="PENDING") # PENDING, IN_PROGRESS, BLOCKED_APPROVAL, COMPLETED, FAILED
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class WorkflowStep(Base):
    \"\"\"Individual step within a workflow.\"\"\"

    __tablename__ = "workflow_steps"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    workflow_id = Column(String, ForeignKey("workflows.id"), nullable=False)
    description = Column(String, nullable=False)
    tool_name = Column(String, nullable=True)
    status = Column(String, default="PENDING")
    requires_approval = Column(Boolean, default=False)
    result = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
"""

if "class Workflow(Base):" not in content:
    content += workflow_models
    with open(file_path, "w") as f:
        f.write(content)
    print("Workflow models added to models.py")
else:
    print("Workflow models already exist.")
