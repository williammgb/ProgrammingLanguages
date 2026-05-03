from pathlib import Path
import json
import base64

from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="Validation API", version="1.0.0")

DATA_DIR = Path(__file__).parent
USERS_FILE = DATA_DIR / "data" / "users.json"
LOGS_FILE = DATA_DIR / "data" / "logs.json"


class UserPayload(BaseModel):
    name: str
    password: str
    role: str
    password_mode: str


def load_data(file_path: Path):
    if not file_path.exists():
        return []
    with open(file_path, "r", encoding="utf-8") as fh:
        return json.load(fh)


def save_logs(logs):
    with open(LOGS_FILE, "w", encoding="utf-8") as fh:
        json.dump(logs, fh, indent=2)


@app.post("/validate")
def validate(payload: UserPayload):
    """
    Validates user credentials against the USERS list.
    Performs password validation based on the password_mode.
    """
    mode = payload.password_mode.lower().strip()
    users = load_data(USERS_FILE)
    logs = load_data(LOGS_FILE)

    if mode == "plaintext":
        for u in users:
            if (
                u["name"] == payload.name
                and u["password"] == payload.password
                and u["role"] == payload.role
            ):
                logs.append(
                    {"user": payload.name, "level": "INFO", "event": "validation_success"}
                )
                save_logs(logs)
                return {"valid": True, "mode": "plaintext"}
        logs.append(
            {"user": payload.name, "level": "ERROR", "event": "validation_failed"}
        )
        save_logs(logs)
        return {"valid": False, "mode": "plaintext"}

    if mode == "base64":
        try:
            decoded_password = base64.b64decode(payload.password).decode("utf-8")
        except Exception:
            return {"valid": False, "mode": "base64"}

        for u in users:
            if (
                u["name"] == payload.name
                and u["password"] == decoded_password
                and u["role"] == payload.role
            ):
                logs.append(
                    {"user": payload.name, "level": "INFO", "event": "validation_success"}
                )
                save_logs(logs)
                return {"valid": True, "mode": "base64"}
        logs.append(
            {"user": payload.name, "level": "ERROR", "event": "validation_failed"}
        )
        save_logs(logs)
        return {"valid": False, "mode": "base64"}

    logs.append(
        {"user": payload.name, "level": "ERROR", "event": "validation_failed"}
    )
    save_logs(logs)
    return {"valid": False, "mode": "unknown"}


@app.get("/logs")
def logs(user: str):
    all_logs = load_data(LOGS_FILE)
    return [x for x in all_logs if x["user"] == user]