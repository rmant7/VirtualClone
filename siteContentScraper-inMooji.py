import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin
import time
import os

# ----------- Settings -----------
BASE_URL = "https://mooji.org/sitemap_index.xml"  # sitemap start
OUTPUT_PDF_TXT = "mooji-pdf_txt_links.txt"
OUTPUT_VIDEO = "mooji-video_links.txt"
OUTPUT_YOUTUBE = "mooji-youtube_links.txt"
WAIT_BETWEEN = 0.5  # wait between sitemaps (seconds)
# --------------------------------

# Reset output files
for file in [OUTPUT_PDF_TXT, OUTPUT_VIDEO, OUTPUT_YOUTUBE]:
    if not os.path.exists(file):
        open(file, "w").close()

all_pdf_txt_links = []
all_video_links = []
all_youtube_links = []

def get_sitemaps(url):
    """Fetch sitemap links"""
    try:
        response = requests.get(url, timeout=10)
        soup = BeautifulSoup(response.content, "xml")
        return [
            loc.text
            for loc in soup.find_all("loc")
            if not loc.text.lower().endswith(
                (".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".mp4", ".webm", ".mov")
            )
        ]
    except Exception as e:
        print(f"[ERROR] Could not read sitemap: {url} -> {e}")
        return []

def extract_files_from_page(page_url):
    """Extract PDF, TXT, Video and YouTube links from page"""
    pdf_txt = []
    video = []
    youtube = []
    try:
        response = requests.get(page_url, timeout=10)
        soup = BeautifulSoup(response.text, "html.parser")
        for a_tag in soup.find_all("a", href=True):
            href = a_tag["href"]
            full_url = urljoin(page_url, href)
            href_lower = href.lower()

            # PDF/TXT
            if href_lower.endswith((".pdf", ".txt")):
                pdf_txt.append(full_url)

            # Video (mp4, mkv, avi, mov, webm)
            if any(href_lower.endswith(ext) for ext in [".mp4", ".mkv", ".avi", ".mov", ".webm"]):
                video.append(full_url)

            # YouTube
            if "youtube.com/watch" in href_lower or "youtu.be/" in href_lower:
                youtube.append(full_url)

    except Exception as e:
        print(f"[ERROR] Could not read page: {page_url} -> {e}")
    return pdf_txt, video, youtube

# ---- Scan sitemaps ----
sitemaps = get_sitemaps(BASE_URL)
total = len(sitemaps)
print(f"[INFO] Found {total} sub-sitemaps.")

for idx, sitemap_url in enumerate(sitemaps):
    progress = int((idx + 1) / total * 100)
    print(f"[PROGRESS] {progress}% - {sitemap_url}")

    page_urls = get_sitemaps(sitemap_url)
    found_pdf_txt = []
    found_video = []
    found_youtube = []

    for page_url in page_urls:
        pdf_txt_links, video_links, youtube_links = extract_files_from_page(page_url)
        if pdf_txt_links:
            found_pdf_txt.extend(pdf_txt_links)
        if video_links:
            found_video.extend(video_links)
        if youtube_links:
            found_youtube.extend(youtube_links)

    # ---- Write PDF/TXT links ----
    if found_pdf_txt:
        all_pdf_txt_links.extend(found_pdf_txt)
        with open(OUTPUT_PDF_TXT, "a", encoding="utf-8") as f:
            f.write(f"\nSource: {sitemap_url}\n")
            for i, link in enumerate(found_pdf_txt, 1):
                f.write(f" - {link}\n")
                print(f"[PDF/TXT] [{i}/{len(found_pdf_txt)}] Link saved: {link}")
        print(f"[INFO] {len(found_pdf_txt)} PDF/TXT links saved.")

    # ---- Write Video links ----
    if found_video:
        all_video_links.extend(found_video)
        with open(OUTPUT_VIDEO, "a", encoding="utf-8") as f:
            f.write(f"\nSource: {sitemap_url}\n")
            for i, link in enumerate(found_video, 1):
                f.write(f" - {link}\n")
                print(f"[VIDEO] [{i}/{len(found_video)}] Link saved: {link}")
        print(f"[INFO] {len(found_video)} Video links saved.")

    # ---- Write YouTube links ----
    if found_youtube:
        all_youtube_links.extend(found_youtube)
        with open(OUTPUT_YOUTUBE, "a", encoding="utf-8") as f:
            f.write(f"\nSource: {sitemap_url}\n")
            for i, link in enumerate(found_youtube, 1):
                f.write(f" - {link}\n")
                print(f"[YOUTUBE] [{i}/{len(found_youtube)}] Link saved: {link}")
        print(f"[INFO] {len(found_youtube)} YouTube links saved.")

    time.sleep(WAIT_BETWEEN)

# ---- Summary ----
print(f"\n✅ Done. Found {len(all_pdf_txt_links)} PDF/TXT, {len(all_video_links)} Video and {len(all_youtube_links)} YouTube links.")
