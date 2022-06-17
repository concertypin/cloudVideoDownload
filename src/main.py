import download
from enum import Enum
from typing import Union
from fastapi import FastAPI


hostname = r"http://localhost:8000"
app = FastAPI()
queue = download.QueueManagement()


@app.get("/")
def read_root():
    return f"This is root of the page, which isn't reserved as an endpoint. Watch our docs in {hostname}/docs ."


@app.get("/watchQueue/{compact}")
def read_queue(compact: bool):
    return queue.watch_queue(compact)


@app.get("/appendQueue/{url}")
def append_queue(url: str):
    being_queued = download.DownloadingQueue(url)
    queue.registry(being_queued)
    return being_queued.lookup()


@app.get("/downloadAll")
def download_all():
    queue.start_download()
    return {"status": "successed", "pending_videos": len(queue.queue)}
