package fr.uga.l3miage.library.books;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.data.domain.Book.Language;
import fr.uga.l3miage.library.authors.AuthorDTO;
import fr.uga.l3miage.library.authors.AuthorMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Calendar;
import java.util.Collection;

import java.util.Set;

@RestController
@RequestMapping(value = "/api", produces = "application/json")
public class BooksController {

    private final BookService bookService;
    private final BooksMapper booksMapper;
    private final AuthorService authorService;
    private final AuthorMapper authorMapper;

    @Autowired
    public BooksController(BookService bookService, BooksMapper booksMapper, AuthorService authorService,
            AuthorMapper authorMapper) {
        this.bookService = bookService;
        this.booksMapper = booksMapper;
        this.authorService = authorService;
        this.authorMapper = authorMapper;

    }

    @GetMapping("/v1/books")
    public Collection<BookDTO> books(@RequestParam(value = "q", required = false) String query) {
        Collection<Book> books;
        if (query == null) {
            books = bookService.list();
        } else {
            books = bookService.findByTitle(query);
        }
        return books.stream()
                .map(booksMapper::entityToDTO)
                .toList();
    }

    @GetMapping("/v1/authors/{authorId}/books")
    public Collection<BookDTO> bookByAuthor(@PathVariable("authorId") Long id) throws EntityNotFoundException {
        var author = this.authorService.get(id);
        var books = author.getBooks();
        return booksMapper.entityToDTO(books);
    }

    @GetMapping("/v1/books/{id}")
    public BookDTO book(@PathVariable("id") Long id) throws EntityNotFoundException {
        var book = this.bookService.get(id);
        if (book == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return booksMapper.entityToDTO(book);
    }

    private boolean isValidISBN(Long isbn) {
        // validateur de ISBN il faut qu'il contient au moins 12 chiffres
        return isbn > 0 && String.valueOf(isbn).length() >= 12;
    }

    @PostMapping("/v1/authors/{authorId}/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDTO newBook(@PathVariable("authorId") Long authorId, @RequestBody BookDTO book)
            throws EntityNotFoundException {
        if (book.language() != null) {
            boolean correctLanguage = false;
            for (Language language : Language.values()) {
                if (language.name().equalsIgnoreCase(book.language())) {
                    correctLanguage = true;
                    break;
                }
            }
            if (!correctLanguage) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        }
        Book newBook = booksMapper.dtoToEntity(book);
        Long isbn = newBook.getIsbn();
        String titre = newBook.getTitle();
        String publisher = newBook.getPublisher();
        short year = newBook.getYear();
        Language lang = newBook.getLanguage();

        try {
            Author author = this.authorService.get(authorId);
            if (titre == null || titre.trim().isEmpty() || year < 0 || year > Calendar.getInstance().get(Calendar.YEAR)
                    || isbn == null || !isValidISBN(isbn) || author == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            } else {
                newBook.setYear(year);
                newBook.setIsbn(isbn);
                newBook.setTitle(titre);
                newBook.setLanguage(lang);
                newBook.setAuthors(newBook.getAuthors());
                newBook.setId(newBook.getId());
                newBook.setPublisher(publisher);
            }
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try {
            bookService.save(authorId, newBook);
            return booksMapper.entityToDTO(newBook);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

    }

    @PutMapping("/v1/books/{bookId}")
    public BookDTO updateBook(@PathVariable("bookId") Long bookId, @RequestBody BookDTO book)
            throws EntityNotFoundException {
        // attention BookDTO.id() doit être égale à id, sinon la requête utilisateur est
        // mauvaise
        if (book.id() == bookId) {
            var existingBook = bookService.get(bookId);
            existingBook.setYear(book.year());
            existingBook.setIsbn(book.isbn());
            existingBook.setTitle(book.title());
            existingBook.setId(book.id());
            existingBook.setPublisher(book.publisher());
            bookService.update(existingBook);
            return booksMapper.entityToDTO(existingBook);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/v1/books/{newBookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable("newBookId") Long id) throws EntityNotFoundException {
        var deletedBook = bookService.get(id);
        if (deletedBook == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } else {
            bookService.delete(id);
        }
    }


    @PutMapping("v1/books/{bookId}/authors")
    @ResponseStatus(HttpStatus.OK)
    public BookDTO addAuthor(@PathVariable("bookId") Long bookId, @RequestBody AuthorDTO author) throws EntityNotFoundException, JsonProcessingException {
        Book book = bookService.get(bookId);
        Author addAuthor = authorMapper.dtoToEntity(author);
        addAuthor.setFullName(addAuthor.getFullName());
        addAuthor.setId(addAuthor.getId());
        Set<Author> authors = book.getAuthors();
        book.addAuthor(addAuthor);
        book.setAuthors(authors);
        BookDTO newBook = updateBook(bookId, booksMapper.entityToDTO(book));
        return newBook;
    }   
}