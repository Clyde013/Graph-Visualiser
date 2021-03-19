import pickle

def loadPickle(filepath): # load train / test data from pickle format
    # for reading also binary mode is important 
    dbfile = open(filepath, 'rb')      
    db = pickle.load(dbfile)    #the db is output in a 2d array 
    dbfile.close()  # with the inner one being a dictionary of labels
    return db      # "features" and "label"
    
#loadPickle('./data/test/test.pickle')

def loadClasses(filepath):  # load label from classes.txt and output in array
    classesFile = open(filepath, 'rb')
    labels = []
    for label in classesFile:
        labels.append(label.strip())
    return labels

#loadClasses('classes.txt')    
