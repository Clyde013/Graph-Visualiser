import pickle

def loadPickle(filepath): 
    # for reading also binary mode is important 
    dbfile = open(filepath, 'rb')      
    db = pickle.load(dbfile) 
    for keys in db: 
        print(keys) 
    dbfile.close() 

#loadPickle('./data/test/test.pickle')
