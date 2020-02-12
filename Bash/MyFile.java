//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

public class MyFile extends FileSystemComponent{

    public MyFile(String fileName, FileSystemComponent parentDirectory){
        super(fileName, parentDirectory);
    }

    public String getPathOf(Path path){
	return null;
    }

    public int mkdir(Path path){
        return 1;
    }

    @Override
    public FileSystemComponent getComponentAtPath(Path path, int untreatedComponents) {
        if(path.getSize() == untreatedComponents){
            return this;
        }
        else{
            return null;
        }
    }

    public int cd(Path path){
        return 1;
    }

    @Override
    public int ls(PrintWriter outputFile, Path path, boolean isRecursive) {
        return 1;
    }

    @Override
    public int touch(Path path) {
        return 1;
    }

    @Override
    public int rm(Path path) {
        return 1;
    }


    @Override
    public void copy(Directory destination){
        destination.add(new MyFile(getName(), destination));
    }

    @Override
    public int cp(Path source, Path destination) {
        return 0;
    }

    @Override
    public int mv(Path source, Path destination) {
        return 0;
    }
}
