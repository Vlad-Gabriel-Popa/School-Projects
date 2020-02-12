//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;
import java.util.List;

public class CommandInvoker {
    private ICommand command;
    private static CommandInvoker instance;

    private  CommandInvoker(){

    }
    public static CommandInvoker getInstance(){
        if(instance == null){
            instance = new CommandInvoker();
            return instance;
        }
        return instance;
    }

    public void setCommand(PrintWriter outputFile, PrintWriter errorFile, CurrentDirectory currentDirectory, List<String> parameters){
        //cream un command factory pentru a putea initializa comanda primita ca input
        ICommandFactory commandFactory = CommandFactory.getInstance();
        command = commandFactory.createCommand(outputFile, errorFile, currentDirectory, parameters);

    }

    public void executeCommand(){
        command.execute();
    }

}
