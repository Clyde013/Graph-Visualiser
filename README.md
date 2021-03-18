# Graph-Visualiser
Android application to identify written coordinates of points of a graph on paper and overlay a graph fitting those points. 

Expected flow of code:

1. Image capture
2. Prompt users to manually select the boundaries of xy-axes on image and define their ranges
3. EAST OCR to capture bounding regions of written coordinates
4. Image binarisation (maybe invert colours for better results) for every isoloated coordinate image to remove noise
5. Pass through own model to recognise numbers and special characters (π, e)
6. Form query for wolfram alpha using keyword "fit"
7. Display results overlaid on image capture, scaled to fit the boundaries
8. Profit???

Possible sources for use:

Reference paper: https://stacks.stanford.edu/file/druid:yt916dh6570/Naqvi_Sikora_AR_Equation_Plotter.pdf

Efficient and Accurate Scene Text Detector: https://arxiv.org/pdf/1704.03155.pdf#page=1&zoom=auto,-265,798

Efficient and Accurate Implementation of said Scene Text Detector: https://github.com/argman/EAST

Tesseract OCR from Google: https://github.com/tesseract-ocr/tesseract

Wolfram Alpha example query: https://www.wolframalpha.com/input/?i=fit+%280%2C0%29%2C+%281%2C1%29%2C+%282%2C2%29%2C+%283%2C3%29

Extended MNIST Dataset: https://www.westernsydney.edu.au/icns/reproducible_research/publication_support_materials/emnist
