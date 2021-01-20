package librarypoint;

public class Reviewed extends AbstractEvent {

    private Long id;
    private Long bookName;
    private Long memberId;
    private String bookReview;

    public Reviewed(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getBookName() {
        return bookName;
    }

    public void setBookName(Long bookName) {
        this.bookName = bookName;
    }
    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    public String getBookReview() {
        return bookReview;
    }

    public void setBookReview(String bookReview) {
        this.bookReview = bookReview;
    }
}