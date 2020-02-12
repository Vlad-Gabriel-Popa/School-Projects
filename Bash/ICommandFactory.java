//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;
import java.util.List;

public interface ICommandFactory {
    ICommand createCommand(PrintWriter outputFile, PrintWriter errorFile, CurrentDirectory currentDirectory, List<String> parameters);
}

class CommandFactory implements ICommandFactory {
    private static CommandFactory instance;

    private CommandFactory(){

    }
    // vom asigura existenta unui singur invoker folosind Singleton
    public static CommandFactory getInstance(){
        if(instance == null){
            instance = new CommandFactory();
            return instance;
        }
        return instance;
    }
    //functie ce identifica comanda primita si intoarce comanda cu parametrii primiti
    @Override
    public ICommand createCommand(PrintWriter outputFile, PrintWriter errorFile, CurrentDirectory currentDirectory, List<String> parameters) {
        //primul parametru din lista este numele comenzii
        switch (parameters.get(0)) {
            //daca comanda e ls trebuie sa identificam daca comanda e recursiva
            case "ls":
                String path = "";
                boolean isRecursive = false;
                int i = 1;
                while (i < parameters.size()) {
                    if (parameters.get(i).equalsIgnoreCase("-R")) {
                        isRecursive = true;
                    } else {
                        path = parameters.get(i);
                    }
                    i++;
                }
                return Ls.getInstance(outputFile, errorFile, currentDirectory, path, isRecursive);

            case "pwd":
                return Pwd.getInstance(outputFile, errorFile, currentDirectory);
            case "cd":
                return Cd.getInstance(errorFile, currentDirectory, parameters.get(1));
            case "cp":
                return Cp.getInstance(errorFile, currentDirectory, parameters.get(1), parameters.get(2));
            case "mv":
                return Mv.getInstance(errorFile, currentDirectory, parameters.get(1), parameters.get(2));
            case "rm":
                return Rm.getInstance(errorFile, currentDirectory, parameters.get(1));
            case "touch":
                return Touch.getInstance(errorFile, currentDirectory, parameters.get(1));
            case "mkdir":
                return Mkdir.getInstance(errorFile, currentDirectory, parameters.get(1));
        }
        return null;
    }
}
