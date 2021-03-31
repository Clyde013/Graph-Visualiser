# Graph-Visualiser
Android application to identify written coordinates of points of a graph on paper and overlay a graph fitting those points. 

Expected flow of code:

1. Image capture
2. Prompt users to manually select the boundaries of xy-axes on image and define their ranges
3. ML-Kit to identify bounding boxes
4. Image binarisation (maybe invert colours for better results) for every isoloated coordinate image to remove noise
5. Pass through own model to recognise numbers and special characters (π, e)
6. Form query for wolfram alpha using keyword "fit"
7. Display results overlaid on image capture, scaled to fit the boundaries
8. Profit???

Possible sources for use:

Thanks google: https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart  
Nevermind Google: https://stackoverflow.com/questions/53638369/how-to-detect-single-digit-numbers-with-firebase-ml-kit-on-android  
Use cloud vision since that provides support for bounding box of symbols: https://firebase.google.com/docs/ml/android/recognize-text
Probably use it to identify bounding boxes and then apply my model to it. Check whether numbers are identified as symbols or have to use block's bounding box and separate manually to identify

Reference paper: https://stacks.stanford.edu/file/druid:yt916dh6570/Naqvi_Sikora_AR_Equation_Plotter.pdf 

Efficient and Accurate Scene Text Detector: https://arxiv.org/pdf/1704.03155.pdf#page=1&zoom=auto,-265,798 
Efficient and Accurate Implementation of said Scene Text Detector: https://github.com/argman/EAST 

Tesseract OCR from Google: https://github.com/tesseract-ocr/tesseract 

Wolfram Alpha example query: https://www.wolframalpha.com/input/?i=fit+%280%2C0%29%2C+%281%2C1%29%2C+%282%2C2%29%2C+%283%2C3%29 

CROHME dataset extractor tool: https://github.com/ThomasLech/CROHME_extractor Could use to extract digits, greek mathematical symbols and operators 

Chaquopy Android Studio and Python: https://chaquo.com/chaquopy/doc/current/ 

Notes:
1. Put output/ train and test files from CROHME extractor tool in /python test scripts/coordinate recognition/data
2. Commands run on py37 anaconda env
3. CROHME_Extractor commands:
`python extract.py -b 50 -d 2011 2012 2013 -c digits greek operators -t 5`
`python balance.py -b 50 -ub 6000`
4. EAST commands: `python run_demo_server.py --checkpoint_path tmp/east_icdar2015_resnet_v1_50_rbox` opens http://localhost:8769/ or `python eval.py --test_data_path=/tmp/images/ --gpu_list=0 --checkpoint_path=/tmp/east_icdar2015_resnet_v1_50_rbox/ --output_dir=/tmp/`
 
