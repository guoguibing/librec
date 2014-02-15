The format of configuration file used in HappyCoding Projects: 

    1) String:  [var.name]=[var.string.val], e.g., dataset=Epinions
    2) Boolean: [is.var.name]=[on/off], e.g., is.verbose=on
    3) Integer: [num.var.name]=[int.val], e.g., num.kfold=5; 
        Note that value -1 indicates disable this property, or use as many as possible
    4) Double:  [val.var.name]=[double.val], e.g., val.ratio=0.2
    5) Range:   [val.var.name]=[start..step..end] or [val1, val2, val3], e.g., val.ratio=0.1..0.1..1.0
    6) IO Path: [var.name.wins]=[windows.file.path], and [var.name.lins]=[linux/unix.file.path] for the purpose of easy switch between windows and linuxs
        Note: use key "var.name", 
        e.g., rating.wins=D:\gguo1\ratings.txt; 
              rating.lins=/home/gguo1/ratings.txt
   
How to run: 
    java -jar librec.jar &
    
How to check out source codes: 
	https://github.com/guoguibing/librec