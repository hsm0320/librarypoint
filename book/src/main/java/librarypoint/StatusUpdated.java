package librarypoint;

public class StatusUpdated extends AbstractEvent {

    private Long id;
    private Long bookId;
    private String bookStatus;
    private Long memberID;
    private Long rendtalId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getBookName() {
        return bookId;
    }

    public void setBookName(Long bookId) {
        this.bookId = bookId;
    }
    public String getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }
    public Long getMemberId() {
        return memberID;
    }

    public void setMemberId(Long memberID) {
        this.memberID = memberID;
    }
    public Long getRendtalId() {
        return rendtalId;
    }

    public void setRendtalId(Long rendtalId) {
        this.rendtalId = rendtalId;
    }
}