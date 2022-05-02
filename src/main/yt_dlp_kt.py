import os

AWESOME_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"  # AWESOME(for test)


def download(url: str, video_name: str = r"%(title)s_original.%(ext)s") -> tuple:
    """
    download the video.
    :param url: the url that this func will download.
    :param video_name: target path, template is following https://github.com/yt-dlp/yt-dlp#output-template
    :return: the video's title(except extension) and extension as a tuple-form.
    :raise yt_dlp.utils.DownloadError if URL is unavailable.
    """
    from yt_dlp import YoutubeDL
    with YoutubeDL() as ydl:
        ydl.outtmpl_dict["default"] = video_name
        r = ydl.sanitize_info(ydl.extract_info(url, download=True))
        return r["title"], r["ext"]


# noinspection PyPep8Naming
def normalize(path: str, result_path: str, target: int) -> None:
    """
    normalize the video with specified target volume.
    :param path: input file.
    :param result_path: result file which is normalized with target volume.
    :param target: target volume, its unit is LUFS.
    :return: none.
    """

    import os
    import platform

    audio_temp = os.getenv("temp") + "\\audio.wav"  # the path that rendered file will be in.
    video_temp = os.getenv("temp") + "\\video.mkv"
    normalize_temp = os.getenv("temp") + "\\normalize.wav"

    if platform.system() == "Windows":
        print_remover = " >nul"  # to remove stdout, this string will be appended next to the cmd.
        os.system("cls")  # clear console
    else:
        print_remover = " > /dev/null"
        os.system("clear")
        audio_temp = audio_temp.replace("\\", "/")  # because Linux can't read that \ is the same with /
        video_temp = video_temp.replace("\\", "/")
        normalize_temp = normalize_temp.replace('\\', '/')

    def divide():
        """
        render an audio-only file and a video-only file.
        video-only file rendering way is not just muting audio, but delete whole audio track.
        """
        audio_cmd = f'ffmpeg -i "{path}" -y "{audio_temp}" -loglevel quiet -stats'  # it will render audio
        video_cmd = f'ffmpeg -i "{path}" -c:v copy -y -an "{video_temp}" -loglevel quiet -stats'  # it will render video
        os.system(audio_cmd + print_remover)  # render audio
        os.system(video_cmd + print_remover)  # render video

    def normalize_audio():
        """
        normalize audio file to normalize_temp.
        soundLUFS is the target of normalize(LUFS).
        """
        cmd = f'ffmpeg-normalize "{audio_temp}" -o "{normalize_temp}" -t {target}'
        os.system(cmd + print_remover)

    def merge():
        """
        combine video_temp and normalize_temp to result_path.
        video_temp should be without audio track, because original and normalized audio are played together.
        result_path must be able to accept aac audio.
        normalize should be audio-only file. its opposite wasn't observed.
        """
        cmd = f'ffmpeg -i "{video_temp}" -i "{normalize_temp}"' + \
              f'-c:v copy -c:a aac -y -strict experimental "{result_path}" -loglevel quiet -stats '
        os.system(cmd + print_remover)

    def cleanup():
        """
        delete every temp that were used in rendering.
        """
        os.remove(audio_temp)
        os.remove(video_temp)
        os.remove(normalize_temp)

    divide()
    normalize_audio()
    merge()
    cleanup()


def orchestration(url: str, target: int = -23, delete_original_file: bool = True):
    """
    download url and normalize. output file is <videoName>.<ext> template. original file will be removed.
    output file path will be printed in stdout.
    :param target: target volume, its unit is LUFS.
    :param delete_original_file: will original video (whose name is <videoName>_original.<ext> template) be deleted.
    :param url: URL that will be downloaded.
    :return: None.
    """
    import platform
    video_name, ext = download(url)

    original_filename = "downloadFileResult\\" + video_name + "_original." + ext
    result_filename = "downloadFileResult\\" + video_name + "." + ext, target

    if platform.system() != "Windows":
        original_filename = original_filename.replace("\\", "/")  # because Linux can't read that \ is the same with /
        result_filename = result_filename.replace("\\", "/")

    normalize(original_filename, result_filename)
    if delete_original_file:
        os.remove(result_filename)
