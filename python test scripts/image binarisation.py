from skimage.filters.rank import median
from skimage.morphology import disk
import numpy as np
import matplotlib.pyplot as plt

from skimage import img_as_ubyte
from skimage import data

from skimage import io
from skimage.color import rgb2gray

image = io.imread('testImage.png')
grayscale = img_as_ubyte(rgb2gray(image))

fig, (ax1, ax2, ax3) = plt.subplots(1, 3, figsize=(15, 5))

rankFiltered = median(grayscale, disk(160))

subtracted = np.subtract(rankFiltered.astype(np.int16), grayscale).clip(0, 255).astype(np.uint8)

ax1.imshow(subtracted, vmin=0, vmax=255, cmap=plt.cm.gray)
ax1.axis('off')

ax2.imshow(grayscale, vmin=0, vmax=255, cmap=plt.cm.gray)
ax2.axis('off')

ax3.imshow(rankFiltered, vmin=0, vmax=255, cmap=plt.cm.gray)
ax3.axis('off')

plt.show()
