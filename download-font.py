import urllib.request
import ssl

ssl._create_default_https_context = ssl._create_unverified_context
url = "https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Regular.ttf"

urllib.request.urlretrieve(url, "d:/myproj/shippinggo/src/main/resources/fonts/Amiri-Regular.ttf")
print("Amiri Downloaded!")
