//Popa Vlad-Gabriel 323CB Tema3
public class Path {
    private String[] path;
    //retine numarul de tokeni din path
    private int length;
    //retine cati tokeni au fost cititi din path la un moment dat
    //cand index == length s-a citit tot pathul
    private int index;
    //retine daca un path e relativ
    boolean isRelative;
    //stocheaza intreg path-ul sub forma de string
    private String fullPath;

    //se retine fiecare token dintr-un path in cate o celula a unui vector(path)
    public Path(String path){

        //functia spllit nu merge pentru pathul "/" asa ca tratez separat cazul
        if(path.equals("/")){
            this.path = new String[1];
            this.path[0] = "";
        }else {
            this.path = path.split("/");
        }
        index = 0;
	this.length = this.path.length;
        fullPath = path;
        //daca primim un string vid (adica ""), il eliminam din path
        if(path.equals("")){
            index = 1;
        }
        //testam daca calea e relativa
        isRelative = false;
        if( path.length() > 0 && path.charAt(0) != '/'){
            isRelative = true;
        }
	
    }

    //returneaza token-ul curent din path si trece la urmatorul
    public String getNextToken(){
        if(index >= length){
             return null;
        }
        isRelative = true; // o cale ce incepe sa fie procesata devine relativa
        index++;
        return path[index - 1];
    }

    public int getSize(){
        return length - index;
    }

    public String getFullPath(){
        return fullPath;
    }

    public void eliminateLastToken(){
	length--;		
    }

    //reseteaza parcurgerea path-ului
    public void refresh(){
	index = 0;
	length = path.length;
	isRelative = false;

        if(fullPath.equals("")){
            index = 1;
        }

        if( fullPath.length() > 0 && fullPath.charAt(0) != '/'){
            isRelative = true;
        }

    }

    //returneaza ultimul element din cale fara a altera parametrii
    public String getLastToken() {
        return path[path.length - 1];
    }
}
