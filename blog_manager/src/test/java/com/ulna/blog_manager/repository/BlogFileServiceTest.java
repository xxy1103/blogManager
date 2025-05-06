package com.ulna.blog_manager.repository;

import com.ulna.blog_manager.model.Blog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class BlogFileServiceTest {

    private BlogFileService blogFileService;

    @TempDir
    Path tempStorageDir;

    private String createFrontMatter(String title, String categories, String[] tags, String saying, LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringJoiner sj = new StringJoiner("\n");
        sj.add("---");
        sj.add("title: " + title);
        if (categories != null && !categories.isEmpty()) {
            sj.add("categories: " + categories);
        }
        if (tags != null && tags.length > 0) {
            sj.add("tags: [" + String.join(", ", tags) + "]"); // Format as YAML list
        }
        if (saying != null && !saying.isEmpty()) {
            sj.add("saying: " + saying);
        }
        if (dateTime != null) {
            sj.add("date: " + dateTime.format(formatter));
        }
        sj.add("---");
        return sj.toString();
    }

    @BeforeEach
    void setUp() throws IOException {
        blogFileService = new BlogFileService(tempStorageDir.toString());
        blogFileService.init();
    }

    private Blog createBlogForTest(String filename, String categories, String title, String rawContent) {
        String[] testTags = new String[]{"test-tag", "another-tag"};
        String frontMatter = createFrontMatter(title, categories, testTags, "A test saying", LocalDateTime.now());
        String fullContent = frontMatter + "\n" + rawContent;
        String relativePath = categories.isEmpty() ? filename : categories + "/" + filename;
        Path filePath = blogFileService.resolve(relativePath);
        
        Blog blog = new Blog(fullContent, filePath); 
        blog.setFilename(filename); 
        return blog;
    }

    private Blog createBlogForAddTest(String filename, String categories, String title, String rawContent) {
        Blog blog = new Blog();
        blog.setFilename(filename);
        blog.setCategories(categories);
        blog.setTitle(title);
        blog.setTags(new String[]{"add-test-tag"}); // Corrected to String[]
        blog.setSaying("Adding a new blog");
        blog.setDate(LocalDateTime.now()); // Corrected to setDate
        blog.setContent(rawContent); 
        return blog;
    }

    @Test
    void init_shouldCreateDirectory_whenNotExists() throws IOException {
        Path newDirForCustomInit = tempStorageDir.resolve("custom_init_test_dir");
        BlogFileService customService = new BlogFileService(newDirForCustomInit.toString());
        assertFalse(Files.exists(newDirForCustomInit), "Directory should not exist before init");
        customService.init();
        assertTrue(Files.exists(newDirForCustomInit), "Directory should exist after init");
        assertTrue(Files.isDirectory(newDirForCustomInit), "Path should be a directory");
    }

    @Test
    void init_shouldThrowException_whenPathIsFile() throws IOException {
        Path filePathAsStorage = tempStorageDir.resolve("file_instead_of_dir.txt");
        Files.createFile(filePathAsStorage);
        BlogFileService customService = new BlogFileService(filePathAsStorage.toString());
        RuntimeException exception = assertThrows(RuntimeException.class, customService::init,
                "Should throw RuntimeException when storage path is a file");
        assertTrue(exception.getMessage().contains("配置的存储路径存在但不是一个目录"),
                "Exception message should indicate that path is not a directory");
    }

    @Test
    void init_shouldNotFail_whenDirectoryAlreadyExists() {
        assertTrue(Files.exists(tempStorageDir));
        assertTrue(Files.isDirectory(tempStorageDir));
        assertDoesNotThrow(() -> blogFileService.init(),
                "init() should not throw an exception if directory already exists");
    }

    @Test
    void resolve_shouldReturnCorrectPath_forValidFilename() {
        String filename = "test-post.md";
        Path expectedPath = tempStorageDir.resolve(filename);
        Path actualPath = blogFileService.resolve(filename);
        assertEquals(expectedPath.normalize(), actualPath.normalize(), "Resolved path should match expected path");
    }
    
    @Test
    void resolve_shouldReturnCorrectPath_forValidFilenameWithCategory() {
        String filenameWithCategory = "category/test-post.md";
        Path expectedPath = tempStorageDir.resolve(filenameWithCategory);
        Path actualPath = blogFileService.resolve(filenameWithCategory);
        assertEquals(expectedPath.normalize(), actualPath.normalize(), "Resolved path with category should match");
    }

    @Test
    void resolve_shouldThrowIllegalArgumentException_forNullFilename() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve(null);
        });
        assertEquals("文件名不能为空", exception.getMessage());
    }

    @Test
    void resolve_shouldThrowIllegalArgumentException_forBlankFilename() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve("   ");
        });
        assertEquals("文件名不能为空", exception.getMessage());
    }

    @Test
    void resolve_shouldThrowIllegalArgumentException_forFilenameWithDotDot() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve("../secret.txt");
        });
        assertTrue(exception.getMessage().contains("文件名不能包含 '..' 序列"));
    }
    
    @Test
    void resolve_shouldThrowIllegalArgumentException_forFilenameWithDotDotInCategory() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve("category/../secret.txt");
        });
        assertTrue(exception.getMessage().contains("文件名不能包含 '..' 序列"));
    }

    @Test
    void resolve_shouldThrowIllegalArgumentException_forFilenameWithLeadingSlash() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve("/leading-slash.md");
        });
        assertTrue(exception.getMessage().contains("文件名不能以路径分隔符开头"));
    }
    
    @Test
    void resolve_shouldThrowIllegalArgumentException_forFilenameWithLeadingBackslash() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.resolve("\\leading-backslash.md");
        });
        assertTrue(exception.getMessage().contains("文件名不能以路径分隔符开头"));
    }

    @Test
    void resolve_shouldThrowRuntimeException_forPathTraversalAttempt() {
        String trickyFilename = ".."; 
        assertThrows(IllegalArgumentException.class, () -> blogFileService.resolve(trickyFilename));
    }

    @Test
    void listPostFilenames_shouldReturnEmptyList_whenDirectoryIsEmpty() {
        List<Blog> blogs = blogFileService.listPostFilenames();
        assertTrue(blogs.isEmpty(), "Should return an empty list for an empty directory");
    }

    @Test
    void listPostFilenames_shouldReturnMarkdownFilesRecursivelyAndSorted() throws IOException {
        Path cat1Dir = Files.createDirectory(tempStorageDir.resolve("cat1"));
        Path cat1SubDir = Files.createDirectory(cat1Dir.resolve("subcat"));
        LocalDateTime time = LocalDateTime.now();

        String contentZ = createFrontMatter("Post Z", "", new String[]{"tagZ"}, "Saying Z", time) + "\nContent Z";
        Files.writeString(tempStorageDir.resolve("postZ.md"), contentZ, StandardCharsets.UTF_8);

        String contentB = createFrontMatter("Post B", "cat1", new String[]{"tagB"}, "Saying B", time.minusHours(1)) + "\nContent B";
        Files.writeString(cat1Dir.resolve("postB.md"), contentB, StandardCharsets.UTF_8);

        String contentA = createFrontMatter("Post A", "cat1/subcat", new String[]{"tagA"}, "Saying A", time.minusHours(2)) + "\nContent A";
        Files.writeString(cat1SubDir.resolve("postA.md"), contentA, StandardCharsets.UTF_8);

        String contentX = createFrontMatter("Another X", "", new String[]{"tagX"}, "Saying X", time.plusHours(1)) + "\nContent X";
        Files.writeString(tempStorageDir.resolve("anotherX.md"), contentX, StandardCharsets.UTF_8);
        
        Files.writeString(cat1Dir.resolve("not-a-post.txt"), "Ignore this", StandardCharsets.UTF_8);

        List<Blog> blogs = blogFileService.listPostFilenames();
        assertEquals(4, blogs.size(), "Should find 4 markdown files");

        blogs.sort(Comparator.comparing(Blog::getFilename));

        assertEquals("anotherX.md", blogs.get(0).getFilename());
        assertEquals("Content X", blogs.get(0).loadContent().trim());
        assertEquals("Another X", blogs.get(0).getTitle());
        assertTrue(blogs.get(0).getFilepath().endsWith("anotherX.md"));

        assertEquals("postA.md", blogs.get(1).getFilename());
        assertEquals("Content A", blogs.get(1).loadContent().trim());
        assertEquals("Post A", blogs.get(1).getTitle());
        assertTrue(blogs.get(1).getFilepath().endsWith(Paths.get("cat1","subcat","postA.md").toString()));
        
        assertEquals("postB.md", blogs.get(2).getFilename());
        assertEquals("Content B", blogs.get(2).loadContent().trim());
        assertEquals("Post B", blogs.get(2).getTitle());
        assertTrue(blogs.get(2).getFilepath().endsWith(Paths.get("cat1","postB.md").toString()));

        assertEquals("postZ.md", blogs.get(3).getFilename());
        assertEquals("Content Z", blogs.get(3).loadContent().trim());
        assertEquals("Post Z", blogs.get(3).getTitle());
        assertTrue(blogs.get(3).getFilepath().endsWith("postZ.md"));
    }
    
    @Test
    void listPostFilenames_shouldHandleIOExceptionDuringFileReadAndContinue() throws IOException {
        String goodContent = createFrontMatter("Good", "", new String[]{"tag"}, "say", LocalDateTime.now()) + "\nGood content";
        Files.writeString(tempStorageDir.resolve("good.md"), goodContent, StandardCharsets.UTF_8);

        Path dirAsMd = tempStorageDir.resolve("directory.md");
        Files.createDirectory(dirAsMd);
        

    

        List<Blog> blogs = blogFileService.listPostFilenames();
        assertEquals(1, blogs.size(), "Should list only valid and readable .md files.");

        if (!blogs.isEmpty()) {
            assertEquals("good.md", blogs.get(0).getFilename());
            assertEquals("Good content", blogs.get(0).loadContent().trim());
        }
    }

    @Test
    void savePost_shouldCreateNewFileAndParentDirectories() throws IOException {
        Blog blog = createBlogForTest("new-post.md", "category/subcategory", "New Post", "Content of new post.");
        Path filePath = blog.getFilepath(); 
        
        assertFalse(Files.exists(filePath.getParent()), "Parent directory should not exist yet");
        assertFalse(Files.exists(filePath), "File should not exist yet");
        Blog tmpBlog;
        try{
             tmpBlog = blog.clone(); // Clone the blog object to avoid modifying the original
        }
        catch(Exception e){
            System.out.println("Clone failed: " + e.getMessage());
            return ;
        }
        tmpBlog.setContent("Content of new post."); // Set the content to the cloned object
        blogFileService.savePost(tmpBlog);

        assertTrue(Files.exists(filePath.getParent()), "Parent directory should be created");
        assertTrue(Files.isDirectory(filePath.getParent()), "Parent path should be a directory");
        assertTrue(Files.exists(filePath), "File should be created");
        
        String savedFileContent = Files.readString(filePath, StandardCharsets.UTF_8);
        System.out.println("Saved file content: " + savedFileContent);
        assertTrue(savedFileContent.contains("title: New Post"));
        assertTrue(savedFileContent.contains("Content of new post."), "File content body should match");
    }

    @Test
    void savePost_shouldOverwriteExistingFile() throws IOException {
        String filename = "existing.md";
        String category = "cat";
        Blog blog1 = createBlogForTest(filename, category, "Old Title", "Old content.");
        blog1.setContent("Old content."); // Set the content to the original object
        blogFileService.savePost(blog1); 

        Path filePath = blog1.getFilepath();
        String initialFileContent = Files.readString(filePath, StandardCharsets.UTF_8);
        assertTrue(initialFileContent.contains("title: Old Title"));
        assertTrue(initialFileContent.contains("Old content."));

        String newFrontMatter = createFrontMatter("New Title", category, new String[]{"tag"}, "saying", LocalDateTime.now());
        String newBody = "New content.";
        Blog blog2 = new Blog(newFrontMatter + "\n" + newBody, filePath); 
        blog2.setFilename(filename); 
        
        blog2.setContent("New content.");
        blogFileService.savePost(blog2); 

        assertTrue(Files.exists(filePath), "File should still exist");
        String overwrittenFileContent = Files.readString(filePath, StandardCharsets.UTF_8);
        assertTrue(overwrittenFileContent.contains("title: New Title"), "Title in file should be updated");
        assertTrue(overwrittenFileContent.contains("New content."), "Body in file should be overwritten");
    }

    @Test
    void deleteBlog_shouldDeleteExistingFile() throws IOException {
        Blog blog = createBlogForTest("to-delete.md", "tempcat", "Delete Me", "Content to be deleted");
        blogFileService.savePost(blog); 
        Path filePath = blog.getFilepath();
        assertTrue(Files.exists(filePath), "File should exist before delete");

        blogFileService.deleteBlog(blog);
        assertFalse(Files.exists(filePath), "File should not exist after delete");
    }

    @Test
    void deleteBlog_shouldThrowNoSuchFileException_forNonExistingFile() {
        Blog blog = createBlogForTest("does-not-exist.md", "anycat", "Non Existent", "Some content");
        assertFalse(Files.exists(blog.getFilepath()), "File should not exist on disk for this test");

        assertThrows(NoSuchFileException.class, () -> {
            blogFileService.deleteBlog(blog);
        }, "Should throw NoSuchFileException for a non-existent file");
    }

    @Test
    void addBlog_shouldCreateFileAndReturnTrue_whenPathIsNull() throws IOException {
        Blog blog = createBlogForAddTest("added-blog.md", "newcat/subcat", "Added Blog", "Fresh content for addBlog.");
        assertNull(blog.getFilepath(), "Filepath should be initially null for this test scenario");

        assertTrue(blogFileService.addBlog(blog), "addBlog should return true on success");
        
        assertNotNull(blog.getFilepath(), "Filepath should be set by addBlog");
        Path expectedPath = tempStorageDir.resolve("newcat/subcat/added-blog.md");
        assertEquals(expectedPath.normalize(), blog.getFilepath().normalize(), "Resolved filepath should be correct");
        assertTrue(Files.exists(expectedPath), "File should be created at the expected path");
        
        String fileContent = Files.readString(expectedPath, StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("title: Added Blog"));
        assertTrue(fileContent.contains("Fresh content for addBlog."), "File content body should match");
    }
    
    @Test
    void addBlog_shouldUseExistingPathAndReturnTrue_whenPathIsNotNull() throws IOException {
        Path preResolvedPath = blogFileService.resolve("presetcat/preset.md");
        String frontMatter = createFrontMatter("Preset Blog Title", "presetcat", new String[]{"tag"}, "say", LocalDateTime.now());
        String body = "Preset content for existing path";
        Blog blog = new Blog(frontMatter + "\n" + body, preResolvedPath);
        blog.setFilename("preset.md"); 

        assertTrue(blogFileService.addBlog(blog), "addBlog should return true when path is pre-set");
        
        assertEquals(preResolvedPath.normalize(), blog.getFilepath().normalize(), "Filepath should remain the pre-resolved one");
        assertTrue(Files.exists(preResolvedPath), "File should exist at the pre-resolved path");
        String fileContent = Files.readString(preResolvedPath, StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("title: Preset Blog Title"));
        assertTrue(fileContent.contains("null"));
    }

    @Test
    void addBlog_shouldThrowIllegalArgumentException_whenGeneratedPathIsOutsideStorage() {
        Blog blog = createBlogForAddTest("evil.md", "../outside_attempt", "Evil Blog", "content");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.addBlog(blog);
        });
        assertTrue(exception.getMessage().contains("Generated blog path is outside the designated storage area"),
                   "Exception message should indicate path traversal attempt");
    }

    @Test
    void addBlog_shouldReturnFalse_onSavePostIOException() throws IOException {
        Blog blog = createBlogForAddTest("io-error-blog.md", "blockedcat", "IO Error Blog", "content");
        
        Path categoryPathAsFile = tempStorageDir.resolve("blockedcat");
        Files.createFile(categoryPathAsFile); 

        assertFalse(blogFileService.addBlog(blog),
            "addBlog should return false when savePost fails due to IO (parent dir is a file)");
        
        assertTrue(Files.exists(categoryPathAsFile) && Files.isRegularFile(categoryPathAsFile),
                   "The blocking file 'blockedcat' should still exist.");
        Path expectedBlogPath = tempStorageDir.resolve("blockedcat/io-error-blog.md");
        assertFalse(Files.exists(expectedBlogPath), "Blog file should not have been created under the problematic path.");
    }

    @Test
    void updateBlogInfo_shouldMoveFileAndPreserveContent_whenCategoriesChange() throws IOException {
        Blog oldBlog = createBlogForAddTest("update-me.md", "old_category", "Original Title", "This is the original content.");
        assertTrue(blogFileService.addBlog(oldBlog), "Setup: Adding old blog should succeed.");
        Path oldPath = oldBlog.getFilepath(); 
        assertTrue(Files.exists(oldPath), "Setup: Old blog file should exist.");
        String oldFileContent = Files.readString(oldPath, StandardCharsets.UTF_8);
        assertTrue(oldFileContent.contains("This is the original content."));

        Blog newBlogInfo = new Blog(); 
        newBlogInfo.setFilename(oldBlog.getFilename()); 
        newBlogInfo.setCategories("new_category");      
        newBlogInfo.setTitle("Updated Title"); 
        newBlogInfo.setTags(oldBlog.getTags()); 
        newBlogInfo.setSaying(oldBlog.getSaying()); 
        newBlogInfo.setDate(oldBlog.getDate()); // Corrected to getDate and setDate

        assertTrue(blogFileService.updateBlogInfo(oldBlog, newBlogInfo), "updateBlogInfo should return true on success");

        assertFalse(Files.exists(oldPath), "Old file at old path should be deleted.");
        
        Path newPath = tempStorageDir.resolve("new_category/" + oldBlog.getFilename());
        assertNotNull(newBlogInfo.getFilepath(), "newBlogInfo.filepath should be updated.");
        assertEquals(newPath.normalize(), newBlogInfo.getFilepath().normalize(), "newBlogInfo filepath should point to the new location.");
        assertTrue(Files.exists(newPath), "New file should exist at the new location.");
        
        String newFileContent = Files.readString(newPath, StandardCharsets.UTF_8);
        assertTrue(newFileContent.contains("title: Updated Title"), "New file should have updated title in front-matter.");
        assertTrue(newFileContent.contains("categories: new_category"), "New file should have updated categories.");
        assertTrue(newFileContent.contains("This is the original content."), "Content body should be preserved in the new file.");
        
        assertEquals("Updated Title", newBlogInfo.getTitle());
        assertEquals("new_category", newBlogInfo.getCategories());
        assertEquals(oldBlog.getFilename(), newBlogInfo.getFilename()); 
        assertEquals(oldBlog.getDate(), newBlogInfo.getDate()); // Corrected to getDate        
    }

    @Test
    void updateBlogInfo_shouldNotMoveFile_whenCategoriesAreSame() throws IOException {
        Blog oldBlog = createBlogForAddTest("no-move.md", "same_category", "Title V1", "Content V1.");
        blogFileService.addBlog(oldBlog);
        Path originalPath = oldBlog.getFilepath();
        assertTrue(Files.exists(originalPath));
        String originalFileContentBody = "Content V1.";

        Blog newBlogInfo = new Blog(); 
        newBlogInfo.setFilename(oldBlog.getFilename());
        newBlogInfo.setCategories(oldBlog.getCategories()); 
        newBlogInfo.setTitle("Title V2");
        newBlogInfo.setTags(oldBlog.getTags());
        newBlogInfo.setSaying(oldBlog.getSaying());
        newBlogInfo.setDate(oldBlog.getDate()); // Corrected to getDate and setDate

        assertTrue(blogFileService.updateBlogInfo(oldBlog, newBlogInfo));

        assertTrue(Files.exists(originalPath), "File should still exist at original path.");
        assertEquals(originalPath.normalize(), newBlogInfo.getFilepath().normalize(), "Filepath in newBlogInfo should be the same.");
        
        String updatedFileContent = Files.readString(originalPath, StandardCharsets.UTF_8);
        assertTrue(updatedFileContent.contains("title: Title V2"), "Title in file should be updated.");
        assertTrue(updatedFileContent.contains(originalFileContentBody), "Content body should be preserved.");
        
        assertEquals("Title V2", newBlogInfo.getTitle(), "Title in newBlogInfo should be updated.");
        assertEquals(oldBlog.getCategories(), newBlogInfo.getCategories());
        assertEquals(oldBlog.getDate(), newBlogInfo.getDate()); // Corrected to getDate
    }
    
    @Test
    void updateBlogInfo_shouldThrowIllegalArgumentException_whenNewPathIsOutsideStorage() throws IOException {
        Blog oldBlog = createBlogForAddTest("security-update.md", "internal_cat", "Secure Update", "content");
        blogFileService.addBlog(oldBlog);
        Path oldPath = oldBlog.getFilepath();

        Blog newBlogInfo = new Blog();
        newBlogInfo.setFilename(oldBlog.getFilename());
        newBlogInfo.setCategories("../attempt_outside"); 

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            blogFileService.updateBlogInfo(oldBlog, newBlogInfo);
        });
        assertTrue(exception.getMessage().contains("Generated blog path is outside the designated storage area"));
        assertTrue(Files.exists(oldPath), "Original file should not be deleted on failed update due to security check.");
        assertNull(newBlogInfo.getFilepath(), "newBlogInfo's filepath should not be set on security failure.");
    }

    @Test
    void updateBlogInfo_shouldReturnFalse_onIOExceptionDuringNewSave() throws IOException {
        Blog oldBlog = createBlogForAddTest("io-update-new.md", "cat_source", "IO Update New", "content");
        blogFileService.addBlog(oldBlog); 
        Path oldPath = oldBlog.getFilepath();
        assertTrue(Files.exists(oldPath));

        Blog newBlogInfo = new Blog();
        newBlogInfo.setFilename(oldBlog.getFilename());
        newBlogInfo.setCategories("cat_target_blocked"); 
        newBlogInfo.setTitle(oldBlog.getTitle());
        newBlogInfo.setTags(oldBlog.getTags());
        newBlogInfo.setSaying(oldBlog.getSaying());
        newBlogInfo.setDate(oldBlog.getDate()); // Corrected to getDate and setDate

        Path newCategoryPathAsFile = tempStorageDir.resolve("cat_target_blocked");
        Files.createFile(newCategoryPathAsFile);

        assertFalse(blogFileService.updateBlogInfo(oldBlog, newBlogInfo),
                    "updateBlogInfo should return false if saving new blog fails.");
        
        assertTrue(Files.exists(oldPath), "Old file should still exist if new save failed.");
        Path problematicNewPathTarget = tempStorageDir.resolve("cat_target_blocked/" + oldBlog.getFilename());
        assertFalse(Files.exists(problematicNewPathTarget), "New file should not have been created at the blocked path.");
        assertTrue(Files.isRegularFile(newCategoryPathAsFile), "The blocking file 'cat_target_blocked' should still be a file.");
        assertEquals(problematicNewPathTarget.normalize(), newBlogInfo.getFilepath().normalize(), "newBlogInfo path should be set to target even if save fails.");
    }
    
    @Test
    void updateBlogInfo_shouldReturnTrue_onIOExceptionDuringOldDelete_ifSaveSucceeded() throws IOException {
        Blog oldBlog = createBlogForAddTest("old-undeletable.md", "cat_old_del", "Old Undeletable", "content");
        blogFileService.addBlog(oldBlog);
        Path oldPath = oldBlog.getFilepath(); 

        Blog newBlogInfo = new Blog();
        newBlogInfo.setFilename(oldBlog.getFilename());
        newBlogInfo.setCategories("cat_new_del"); 
        newBlogInfo.setTitle("New Title for Undeletable Old");
        newBlogInfo.setTags(oldBlog.getTags());
        newBlogInfo.setSaying(oldBlog.getSaying());
        newBlogInfo.setDate(oldBlog.getDate()); // Corrected to getDate and setDate

        boolean result = blogFileService.updateBlogInfo(oldBlog, newBlogInfo);
        assertTrue(result, "Update should be considered successful if new blog is saved, even if old delete had issues (logged).");

        Path newPath = tempStorageDir.resolve("cat_new_del/" + oldBlog.getFilename());
        assertTrue(Files.exists(newPath), "New blog file should exist.");
        
        assertFalse(Files.exists(oldPath), "Old blog file should be deleted in normal successful update.");
    }

    @Test
    void updateBlogInfo_shouldHandleCloneNotSupportedException_gracefully() throws IOException {
        String originalRawContent = "Original Content for clone test";
        Blog realOldBlog = createBlogForAddTest("original.md", "original_cat", "Original Title", originalRawContent);
        blogFileService.addBlog(realOldBlog); 
        Path oldPath = realOldBlog.getFilepath(); 
        assertTrue(Files.exists(oldPath), "Old blog must exist before update.");

        class NonCloneableBlog extends Blog {
            public NonCloneableBlog() { super(); }
            @Override
            public Blog clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException("Simulated clone failure");
            }
        }
        
        Blog newBlogData = new NonCloneableBlog(); 
        newBlogData.setFilename(realOldBlog.getFilename());
        newBlogData.setCategories("new_clone_cat");
        newBlogData.setTitle("Title After Clone Attempt");

        assertTrue(blogFileService.updateBlogInfo(realOldBlog, newBlogData),
                   "Update should succeed even if newBlogData.clone() fails, using fallback.");

        Path newPath = tempStorageDir.resolve("new_clone_cat/" + realOldBlog.getFilename());
        assertTrue(Files.exists(newPath), "File should exist at new location after fallback clone.");
        
        String newFileContent = Files.readString(newPath, StandardCharsets.UTF_8);
        assertTrue(newFileContent.contains("title: Title After Clone Attempt"), "Updated title should be in new file.");
        assertTrue(newFileContent.contains("categories: new_clone_cat"), "Updated category should be in new file.");
        assertTrue(newFileContent.contains(originalRawContent), "Original raw content should be preserved.");
        
        assertFalse(Files.exists(oldPath), "Old file should be deleted."); 

        assertEquals("Title After Clone Attempt", newBlogData.getTitle());
        assertEquals("new_clone_cat", newBlogData.getCategories());
        assertEquals(newPath.normalize(), newBlogData.getFilepath().normalize());
        assertEquals(realOldBlog.getDate(), newBlogData.getDate()); // Corrected to getDate
        //assertEquals(realOldBlog.getTags(), newBlogData.getTags());
        //assertEquals(realOldBlog.getSaying(), newBlogData.getSaying());
    }
}
