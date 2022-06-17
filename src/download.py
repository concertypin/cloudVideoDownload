from select import select
import yt_dlp
from copy import deepcopy


class DownloadingQueue:
    def __init__(self, url, normalize_vol=-23) -> None:
        self.url = url
        self.nomalize_vol = normalize_vol
        self.is_finished = False

    def lookup(self):
        ydl_opts = {}
        with yt_dlp.YoutubeDL({}) as ydl:
            info = ydl.extract_info(self.url, download=False)
            return ydl.sanitize_info(info)

    def download(self):
        with yt_dlp.YoutubeDL({}) as ydl:
            info = ydl.extract_info(self.url, download=True).sanitize_info(info)
            return info


class QueueManagement:
    def __init__(self) -> None:
        self.queue: DownloadingQueue = []

    def gc(self) -> None:
        temp_queue = deepcopy(self.queue)
        offset = 0

        for i in range(len(self.queue)):
            if not (self.queue[i].is_finished):
                # it is already finished, so it is removable
                del temp_queue[i - offset]
        self.queue = temp_queue

    def registry(self, object: DownloadingQueue) -> None:
        self.queue.append(object)

    def start_download(self):
        self.gc()
        # TODO

    def watch_queue(self, compact=False):
        response = []
        self.gc()
        for order in range(len(self.queue)):
            temp = {}
            temp["order"] = order + 1
            temp["url"] = self.queue[order]
