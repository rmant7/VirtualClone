from pytube import Channel
from pytube.exceptions import RegexMatchError
from bs4 import BeautifulSoup
import requests
import ssl
import certifi


try:
    _create_unverified_https_context = ssl._create_unverified_context
except AttributeError:
    pass
else:
    ssl._create_default_https_context = _create_unverified_https_context

ssl_context = ssl.create_default_context(cafile=certifi.where())


def resolve_channel_url(url):

    if url.startswith("@"):
        url = f"https://www.youtube.com/{url}"

    try:
        response = requests.get(url)
        if response.status_code != 200:
            print("❌ Failed to retrieve URL.")
            return None
        soup = BeautifulSoup(response.text, "html.parser")

        canonical = soup.find("link", rel="canonical")
        if canonical and "/channel/" in canonical["href"]:
            return canonical["href"]
        else:
            print("❌ Could not resolve to a channel ID URL.")
            return None
    except Exception as e:
        print(f"❌ Error resolving URL: {e}")
        return None


def is_valid_input(url):

    if url.startswith("https://www.youtube.com") or \
            url.startswith("http://www.youtube.com") or \
            url.startswith("www.youtube.com") or \
            url.startswith("@") or \
            "/channel/" in url:
        return True
    return False


def main():
    while True:
        url = input("Please enter the YouTube channel URL or handle: ").strip()

        if not is_valid_input(url):
            print("❌ Invalid input. Please enter a valid YouTube channel URL or handle (e.g., https://www.youtube.com/@handle or https://www.youtube.com/channel/ID).")
            continue

        resolved_url = resolve_channel_url(url)
        if resolved_url is None:
            print("❌ Could not resolve channel URL. Please try again.")
            continue

        try:
            channel = Channel(resolved_url)
            print(f"✅ Successfully fetched channel: {channel.channel_name}")

            break
        except RegexMatchError:
            print("❌ Invalid channel URL format for pytube. Please try again.")
        except Exception as e:
            print(f"❌ Unexpected error: {e}")
            break


if __name__ == "__main__":
    main()




