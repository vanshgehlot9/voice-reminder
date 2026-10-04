import os

code = """from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from ..database.session import get_db
from ..database.models import Workflow, WorkflowStep
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

router = APIRouter(prefix="/api/mobile/workflows", tags=["workflows"])

class WorkflowStepSchema(BaseModel):
    id: str
    description: str
    status: str
    requires_approval: bool

class WorkflowSchema(BaseModel):
    id: str
    goal: str
    status: str
    steps: List[WorkflowStepSchema] = []

@router.get("", response_model=List[WorkflowSchema])
def get_workflows(db: Session = Depends(get_db)):
    workflows = db.query(Workflow).all()
    result = []
    for w in workflows:
        steps = db.query(WorkflowStep).filter(WorkflowStep.workflow_id == w.id).all()
        step_schemas = [WorkflowStepSchema(id=s.id, description=s.description, status=s.status, requires_approval=s.requires_approval) for s in steps]
        result.append(WorkflowSchema(id=w.id, goal=w.goal, status=w.status, steps=step_schemas))
    return result

@router.post("/{workflow_id}/approve")
def approve_workflow(workflow_id: str, db: Session = Depends(get_db)):
    workflow = db.query(Workflow).filter(Workflow.id == workflow_id).first()
    if not workflow:
        raise HTTPException(status_code=404, detail="Workflow not found")
    
    # Approve the first pending step that requires approval
    step = db.query(WorkflowStep).filter(WorkflowStep.workflow_id == workflow_id, WorkflowStep.status == "PENDING", WorkflowStep.requires_approval == True).first()
    if step:
        step.requires_approval = False
        step.status = "APPROVED"
        workflow.status = "IN_PROGRESS"
        db.commit()
        return {"success": True, "message": "Step approved"}
    
    return {"success": False, "message": "No pending steps require approval"}
"""

with open("/Users/vanshgehlot/voicebox-main/backend/routes/workflows.py", "w") as f:
    f.write(code)
print("Workflows route written.")
