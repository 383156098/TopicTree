package pers.joe;

public class TopicException extends RuntimeException{


    public TopicException(String message){
        super(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
