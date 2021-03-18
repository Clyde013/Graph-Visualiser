# Graph-Visualiser
Android application to identify written coordinates of points of a graph on paper and overlay a graph fitting those points. 

Expected flow of code:

Image capture -> EAST OCR to capture bounding regions of written coordinates -> Tesseract OCR to recognise numbers written -> 

Possible sources for use:

Reference paper: https://stacks.stanford.edu/file/druid:yt916dh6570/Naqvi_Sikora_AR_Equation_Plotter.pdf

Efficient and Accurate Scene Text Detector: https://arxiv.org/pdf/1704.03155.pdf#page=1&zoom=auto,-265,798

Efficient and Accurate Implementation of said Scene Text Detector: https://github.com/argman/EAST

Tesseract OCR from Google: https://github.com/tesseract-ocr/tesseract

Wolfram Alpha example query: https://www.wolframalpha.com/input/?i=fit+%280%2C0%29%2C+%281%2C1%29%2C+%282%2C2%29%2C+%283%2C3%29
