import re
from filter_videos import description


emails = re.findall(r'[\w\.-]+@[\w\.-]+', description)
print("Emails found:", emails)

telegrams = re.findall(r'@[\w\d_]+', description)
print("Telegram or other handles:", telegrams)
