def download(url: str, video_name: str = r"%(title)s_original.%(ext)s") -> None:
    """
    
    :param url: the url that this func will download
    :param video_name: target path, template is following https://github.com/yt-dlp/yt-dlp#output-template
    :return: none
    :raise yt_dlp.utils.DownloadError if URL is unavailable
    """
    from yt_dlp import YoutubeDL
    with YoutubeDL() as ydl:
        ydl.outtmpl_dict["default"] = video_name
        r = ydl.download([url])
        print(r)


def normalize(path: str, result_path: str, soundLUFS: int = -23) -> None:
    """
    
    :param path: original file
    :param result_path: result file
    :param soundLUFS: target vol
    :return: none
    """

    import os
    import platform

    audio_temp = os.getenv("temp") + "\\audio.wav"
    normalize_temp = os.getenv("temp") + "\\normalize.wav"
    video_temp = os.getenv("temp") + "\\video.mkv"

    if platform.system() == "Windows":
        print_remover = " >nul"
        os.system("cls")
    else:
        print_remover = " > /dev/null"
        os.system("clear")

    def devide():
        audio_cmd = f'ffmpeg -i "{path}" -y "{audio_temp}" -loglevel quiet -stats'
        video_cmd = (
            f'ffmpeg -i "{path}" -c:v copy -y -an "{video_temp}" -loglevel quiet -stats'
        )
        os.system(audio_cmd + print_remover)
        os.system(video_cmd + print_remover)

    def normalize_audio():
        cmd = f'ffmpeg-normalize "{audio_temp}" -o "{normalize_temp}" -t {soundLUFS}'
        os.system(cmd + print_remover)

    def merge():
        cmd = f'ffmpeg -i "{video_temp}" -i "{normalize_temp}" -c:v copy -c:a aac -y -strict experimental "{result_path}" -loglevel quiet -stats'
        os.system(cmd + print_remover)

    def cleanup():
        os.remove(audio_temp)
        os.remove(video_temp)
        os.remove(normalize_temp)

    devide()
    normalize_audio()
    merge()
    cleanup()
