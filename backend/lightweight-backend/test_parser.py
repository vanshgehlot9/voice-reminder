import re, datetime
import zoneinfo

def parse_spoken_command(query: str, tz_name: str = "Asia/Kolkata"):
    try:
        tz = zoneinfo.ZoneInfo(tz_name)
    except Exception:
        tz = zoneinfo.ZoneInfo("UTC")
    now = datetime.datetime.now(tz)
    q = query.strip()
    ql = q.lower()
    
    # Check if greeting or generic chat
    if ql in ["hello", "hey", "hi", "siri", "jarvis", "ok google", "hello assistant", "hey assistant", "hey voicereminder", "help"]:
        return {
            "success": True,
            "transcript": q,
            "intent": "chat",
            "task": None,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": "Hello! I am your AI Voice Assistant. How can I help you today?",
            "confidence": 1.0,
            "error": None
        }
    
    if any(p in ql for p in ["who are you", "what can you do", "what are you"]):
        return {
            "success": True,
            "transcript": q,
            "intent": "chat",
            "task": None,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": "I can set reminders, schedule tasks, and keep you organized with voice commands in English and Hindi.",
            "confidence": 1.0,
            "error": None
        }

    # Extract target date/time
    target_dt = None
    date_expr = None
    time_expr = None
    
    # Check relative minutes / hours
    rel_match = re.search(r"in\s+(\d+)\s+(minute|min|hour|hr)s?", ql)
    if rel_match:
        val = int(rel_match.group(1))
        unit = rel_match.group(2)
        if "min" in unit:
            target_dt = now + datetime.timedelta(minutes=val)
            time_expr = f"in {val} minutes"
        else:
            target_dt = now + datetime.timedelta(hours=val)
            time_expr = f"in {val} hours"
    
    # Check day
    target_date = now.date()
    if "tomorrow" in ql or "kal" in ql:
        target_date = now.date() + datetime.timedelta(days=1)
        date_expr = "tomorrow"
    elif "day after tomorrow" in ql or "parso" in ql:
        target_date = now.date() + datetime.timedelta(days=2)
        date_expr = "day after tomorrow"
    elif "today" in ql or "aaj" in ql or "tonight" in ql:
        target_date = now.date()
        date_expr = "today"
        
    hour = None
    minute = 0
    if not rel_match:
        # Search for explicit times
        for m in re.finditer(r"(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.|baje)?", ql):
            h_str, min_str, meridian = m.groups()
            h = int(h_str)
            mn = int(min_str) if min_str else 0
            if meridian:
                meridian = meridian.lower().replace(".", "")
                if "pm" in meridian and h < 12:
                    h += 12
                elif "am" in meridian and h == 12:
                    h = 0
                hour = h
                minute = mn
                time_expr = f"{h_str}:{min_str or '00'} {meridian.upper()}"
                break
            elif 1 <= h <= 24 and ("at " in ql[max(0, m.start()-4):m.start()] or "ko " in ql[max(0, m.start()-4):m.start()]):
                if 1 <= h <= 6:
                    h += 12
                hour = h
                minute = mn
                time_expr = f"{h}:00"
                break

    if target_dt is None:
        if hour is not None:
            candidate = datetime.datetime(target_date.year, target_date.month, target_date.day, hour, minute, 0, tzinfo=tz)
            if candidate < now and date_expr is None:
                candidate += datetime.timedelta(days=1)
            target_dt = candidate
        else:
            if date_expr == "tomorrow":
                target_dt = datetime.datetime(target_date.year, target_date.month, target_date.day, 9, 0, 0, tzinfo=tz)
                time_expr = "9:00 AM"
            else:
                target_dt = now + datetime.timedelta(hours=1)
                time_expr = target_dt.strftime("%I:%M %p")

    # Clean task name
    clean_task = re.sub(r"(?i)^(remind me to|set a reminder to|set a reminder for|reminder for|remind me|remind|yaad dilana ki|yaad dilana|schedule|create a task to|task to)\s*", "", q)
    clean_task = re.sub(r"(?i)\s*(tomorrow|today|tonight|day after tomorrow|kal|parso|aaj)\s*", " ", clean_task)
    clean_task = re.sub(r"(?i)\s*(at\s+\d{1,2}(?::\d{2})?\s*(?:am|pm|a\.m\.|p\.m\.|baje)?|\d{1,2}(?::\d{2})?\s*(?:am|pm|a\.m\.|p\.m\.|baje)|in\s+\d+\s+(?:minutes|mins|hours|hrs))\s*", " ", clean_task)
    clean_task = re.sub(r"\s+", " ", clean_task).strip(" .,!?:;")
    
    if not clean_task:
        clean_task = "Reminder"
    else:
        clean_task = clean_task[0].upper() + clean_task[1:]

    time_formatted = target_dt.strftime("%I:%M %p")
    day_formatted = "tomorrow" if target_dt.date() == (now.date() + datetime.timedelta(days=1)) else ("today" if target_dt.date() == now.date() else target_dt.strftime("%b %d"))
    
    # Detect language for reply
    is_hindi = any(w in ql for w in ["kal", "parso", "aaj", "baje", "yaad", "dilana", "karna", "subah", "sham", "jana", "hai"])
    if is_hindi:
        reply = f"Maine {day_formatted} {time_formatted} ko {clean_task} ka reminder set kar diya hai."
    else:
        reply = f"I've set a reminder to {clean_task} for {day_formatted} at {time_formatted}."

    return {
        "success": True,
        "transcript": q,
        "intent": "create_reminder",
        "task": clean_task,
        "date_expression": date_expr or day_formatted,
        "time_expression": time_expr or time_formatted,
        "scheduled_at": target_dt.isoformat(),
        "reply_text": reply,
        "confidence": 0.95,
        "error": None
    }

if __name__ == "__main__":
    for test in [
        "Remind me to submit project report tomorrow at 10 AM",
        "Remind me to buy milk in 30 minutes",
        "Call mom at 8 pm",
        "Kal subah 9 baje gym jana hai",
        "Hello",
        "Who are you"
    ]:
        res = parse_spoken_command(test)
        print(f"Query: {test}")
        print(f"  Intent: {res['intent']} | Task: {res['task']}")
        print(f"  Time: {res['scheduled_at']}")
        print(f"  Reply: {res['reply_text']}\n")
