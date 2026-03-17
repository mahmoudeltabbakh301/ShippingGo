New-Item -ItemType Directory -Force -Path 'd:\myproj\shippinggo\src\main\resources\fonts'
Invoke-WebRequest -Uri 'https://github.com/google/fonts/raw/main/ofl/cairo/Cairo-Regular.ttf' -OutFile 'd:\myproj\shippinggo\src\main\resources\fonts\Cairo-Regular.ttf'
