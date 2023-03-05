package fr.uga.l3miage.library.authors;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.books.BookDTO;
import fr.uga.l3miage.library.books.BooksMapper;
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
import java.util.Collection;
import java.util.Collections;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class AuthorsController {

    private final AuthorService authorService;
    private final AuthorMapper authorMapper;
    private final BooksMapper booksMapper;
    private final BookService bookService;

    @Autowired
    public AuthorsController(AuthorService authorService, AuthorMapper authorMapper, BooksMapper booksMapper,
            BookService bookService) {
        this.authorService = authorService;
        this.authorMapper = authorMapper;
        this.booksMapper = booksMapper;
        this.bookService = bookService;
    }

    @GetMapping("/authors")
    @ResponseStatus(HttpStatus.OK)
    public Collection<AuthorDTO> authors(@RequestParam(value = "q", required = false) String query) {
        Collection<Author> authors;
        if (query == null) {
            authors = authorService.list();
        } else {
            authors = authorService.searchByName(query);
        }
        return authors.stream()
                .map(authorMapper::entityToDTO)
                .toList();
    }

    @GetMapping("/authors/{authorId}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorDTO author(@PathVariable(value = "authorId") Long id) throws EntityNotFoundException {
        try {
            Author author = authorService.get(id);
            if (author == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return authorMapper.entityToDTO(author);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorDTO newAuthor(@RequestBody AuthorDTO author) {

        Author newAuthor = authorMapper.dtoToEntity(author);
        String fullName = newAuthor.getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        newAuthor.setFullName(fullName);
        authorService.save(newAuthor);
        return authorMapper.entityToDTO(newAuthor);
    }

    @PutMapping("/authors/{authorId}")
    public AuthorDTO updateAuthor(@RequestBody AuthorDTO author, @PathVariable("authorId") Long id)
            throws EntityNotFoundException {
        // attention AuthorDTO.id() doit être égale à id, sinon la requête utilisateur
        // est mauvaise
        if (author.id() == id) {
            var existingAuthor = authorService.get(id);
            // mettre à jour l'author trouvé
            existingAuthor.setFullName(author.fullName());
            authorService.update(existingAuthor);
            // Map l'author mis a jour et le return
            return authorMapper.entityToDTO(existingAuthor);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/authors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable("id") Long id) {
        try {
            Author aut = authorService.get(id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        try {
            for (Book book : authorService.get(id).getBooks()) {
                if (book.getAuthors().size() > 1) {
                    bookService.delete(book.getId());
                }
            }
            this.authorService.delete(id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    public Collection<BookDTO> books(Long authorId) {
        return Collections.emptyList();
    }

}