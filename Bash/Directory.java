//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;
import java.util.TreeSet;

public class Directory extends FileSystemComponent {

    //folosim un tree set pentru a retine continutul unui folder sortat lexicografic
    private TreeSet<FileSystemComponent> contents;


    public Directory(String directoryName, FileSystemComponent parentDirectory) {
        super(directoryName, parentDirectory);
        contents = new TreeSet<>();
    }

    //functie ce returneaza folderul/fisierul de la calea obtinuta prin eliminarea
    //ultimelor untreatedComponents elemente din path plecand de la folderul pe care
    //se aplica prima data functia
    @Override
    public FileSystemComponent getComponentAtPath(Path path, int untreatedComponents) {
        //in momentul in care path-ul a ajuns sa aiba numarul de tokenuri netratate inca
        // egal cu numarul cautat(untreated components) cautarea se opreste
        if (path.getSize() == untreatedComponents) return this;

        //daca calea e relativa
        if (path.isRelative) {
            //scoatem urmatoarea componenta din path si o identificam
            String nextComponentName = path.getNextToken();
            if (nextComponentName.equals(".")) {
                return this.getComponentAtPath(path, untreatedComponents);
            }
            if (nextComponentName.equals("..")) {
                //daca se incearca sa se faca .. pe root returnam null
                if (getParentDirectory() == null) return null;
                return getParentDirectory().getComponentAtPath(path, untreatedComponents);
            }
            //daca componenta e un nume de fisier atunci o cautam prin continutul folderului curent
            for (FileSystemComponent component : contents) {
                if (nextComponentName.equals(component.getName())) {
                    return component.getComponentAtPath(path, untreatedComponents);
                }
            }
            return null;
        } else {
            //daca am ajuns la root transformam calea intr-una relativa prin apleul getNextToken
            if (getParentDirectory() == null) {
                path.getNextToken();
                return this.getComponentAtPath(path, untreatedComponents);
            }
            //daca calea e absoluta incercam sa ajungem la root
            return getParentDirectory().getComponentAtPath(path, untreatedComponents);
        }
    }

    // returneaza calea absoluta a unui fisier care se afla la o anumita cale fata de folderul curent
    public String getPathOf(Path path) {
        FileSystemComponent component = getComponentAtPath(path, 0);
        if (component == null) {
            return null;
        }
        return component.getComponentPath();
    }

    //creaza un nou folder
    public int mkdir(Path path) {
        //daca pathul mai contine doar numele noului folder
        //inseamna ca trebuie sa cream folderul in folderul actual
        if (path.getSize() == 1) {
            String newDirectoryName = path.getNextToken();
            //daca un folder cu acelasi nume exista deja atunci se intoarce eroarea 2
            for (FileSystemComponent component : contents) {
                if (newDirectoryName.equals(component.getName())) {
                    return 2;
                }
            }
            Directory newDirectory = new Directory(newDirectoryName, this);
            contents.add(newDirectory);
            return 0;
        }
        //procesam path-ul pentru a ajunge la folderul care trebuie sa contina noul folder
        FileSystemComponent component = getComponentAtPath(path, 1);
        //daca nu se gaseste acest folder intoarcem eroarea 1
        if (component == null) {
            return 1;
        }
        return component.mkdir(path);
    }

    //cautam folderul cu functia getComponentAtPath si schimbam
    // folderul de lucru(currentDirectory) pe acel folder
    public int cd(Path path) {
        if (path.getSize() == 0) {
            currentDirectory.setCurrentDirectory(this);
            return 0;
        }
        FileSystemComponent component = getComponentAtPath(path, 0);
        //intoarcem eroarea 1 daca nu se gaseste folderul
        if (component == null) {
            return 1;
        }
        return component.cd(path);
    }

    //printeaza continutul unui folder
    @Override
    public int ls(PrintWriter outputFile, Path path, boolean isRecursive) {
        //daca s-a ajuns la calea primita ca parametru
        if (path.getSize() == 0) {

            //printam calea absoluta a folderului curent
            String directoryPath = getComponentPath();
            if (getParentDirectory() == null) {
                directoryPath = "/" + directoryPath;
            }
            outputFile.println(directoryPath + ":");

            //printam continutul folderului curent
            if (contents.size() > 0) {
                //am afisat separat primul element pentru a elimina trailing spaces
                FileSystemComponent firstComponent = null;
                firstComponent = contents.first();
                outputFile.print(firstComponent.getComponentPath());
                //apoi am afisat restul continutului
                for (FileSystemComponent component : contents.tailSet(firstComponent, false)) {
                    outputFile.print(" " + component.getComponentPath());
                }
            }
            outputFile.println("\n");
            //daca calea e relativa am apelat recursiv ls pentru toate elementele din folder
            if (isRecursive) {
                for (FileSystemComponent component : contents) {
                    if (component instanceof Directory) {
                        component.ls(outputFile, path, isRecursive);
                    }
                }
            }
            return 0;
        }
        FileSystemComponent component = getComponentAtPath(path, 0);
        if (component == null) {
            return 1;
        }
        return component.ls(outputFile, path, isRecursive);
    }

    //creaza un fisier la calea path
    @Override
    public int touch(Path path) {
        //daca s-a ajuns la calea dorita
        if (path.getSize() == 1) {
            //ne asiguram ca fisierul nu exista deja(eroarea 2) si adaugam noul fisier
            String newFileName = path.getNextToken();
            for (FileSystemComponent component : contents) {
                if (newFileName.equals(component.getName())) {
                    return 2;
                }
            }
            MyFile newFile = new MyFile(newFileName, this);
            contents.add(newFile);
            return 0;
        }
        FileSystemComponent component = getComponentAtPath(path, 1);
        if (component == null) {
            return 1;
        }
        return component.touch(path);
    }

    protected void remove(FileSystemComponent component) {
        contents.remove(component);
    }

    //sterge componenta de la calea path
    @Override
    public int rm(Path path) {
        //daca am ajuns la parintele folderului/fisierului ce trebuie sters
        if (path.getSize() == 1) {
            //incercam sa obtinem folderul/fisierul ce trebuie sters
            FileSystemComponent component = getComponentAtPath(path, 0);
            //daca nu gasim aceasta componenta returnam eroarea 1
            if (component == null) {
                return 1;
            }
            //eliminam componenta doar daca nu contine directorul de lucru in subarbore
            if (component.hasSubComponent(currentDirectory.get()) == false) {
                ((Directory) this).remove(component);
            }
            return 0;
        }
        //incercam sa ajungem la parintele componentei ce trebuie stearsa
        FileSystemComponent component = getComponentAtPath(path, 1);
        if (component == null) {
            return 1;
        }
        return component.rm(path);
    }

    //verifica daca un folder contine o anumita componenta
    public boolean contains(String name) {

        for (FileSystemComponent component : contents) {
            if (name.equals(component.getName())) {
                return true;
            }
        }
        return false;
    }


    public void add(FileSystemComponent component) {
        contents.add(component);
    }

    //copiaza folderul curent intr-un alt folder destinatie
    @Override
    public void copy(Directory destination) {

        Directory newDirectory = new Directory(getName(), destination);

        destination.add(newDirectory);
        for (FileSystemComponent component : contents) {
            component.copy(newDirectory);
        }

    }

    //copiaza componenta de la path-ul sursa in folderul de la path-ul destionatie
    @Override
    public int cp(Path source, Path destination) {
        //cautam componenta de copiat
        FileSystemComponent componentToCopy = getComponentAtPath(source, 0);
        if (componentToCopy == null) {
            return 1;
        }
        //cautam folderul destinatie
        FileSystemComponent destinationFolder = getComponentAtPath(destination, 0);
        if (destinationFolder == null || !(destinationFolder instanceof Directory)) {
            return 2;
        }
        //daca exista deja o componenta cu acelasi nume intoarcem eroarea 3
        if (((Directory) destinationFolder).contains(componentToCopy.getName())) {
            return 3;
        }
        //realizam copierea
        componentToCopy.copy((Directory) destinationFolder);
        return 0;
    }

    //copiaza componenta de la path-ul sursa in folderul de la path-ul destionatie
    @Override
    public int mv(Path source, Path destination) {
        //cautam sursa
        FileSystemComponent componentToMove = getComponentAtPath(source, 0);
        if (componentToMove == null) {
            return 1;
        }
        //cautam destinatia
        FileSystemComponent destinationFolder = getComponentAtPath(destination, 0);
        if (destinationFolder == null || !(destinationFolder instanceof Directory)) {
            return 2;
        }

        //daca folderul destinatie contine o componenta cu acelasi nume intaorcem eroarea 3
        Directory destinationDir = (Directory) destinationFolder;
        if (destinationDir.contains(componentToMove.getName())) {
            return 3;
        }
        //stergem sursa din folderul in care se afla si o adaugam in folderul destinatie
        ((Directory) componentToMove.getParentDirectory()).remove(componentToMove);
        destinationDir.add(componentToMove);
        componentToMove.setParentDirectory(destinationDir);
        return 0;
    }
}
