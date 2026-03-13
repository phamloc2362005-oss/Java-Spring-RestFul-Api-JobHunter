package vn.locpham.jobhunter.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.locpham.jobhunter.domain.reponse.file.ResUploadFileDTO;
import vn.locpham.jobhunter.service.FileService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.StorageException;

@RestController
@RequestMapping("api/v1")
public class FileController {

    @Value("${locpham.upload-file.base-uri}")
    private String baseUri;

    private final FileService fileServive;

    public FileController(FileService fileServive) {
        this.fileServive = fileServive;
    }

    @PostMapping("/files")
    @ApiMessage("Upload a file")
    public ResponseEntity<ResUploadFileDTO> upload(@RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("folder") String folder) throws URISyntaxException, IOException, StorageException {

        // validate
        if (file == null || file.isEmpty()) {
            throw new StorageException("file trống. Vui lòng tải file lên");
        }

        String fileName = file.getOriginalFilename();
        List<String> allowedExtensions = Arrays.asList("pdf", "jpg", "jpeg", "png", "doc", "docx");
        List<String> allowedMimeTypes = Arrays.asList(
                "application/pdf",
                "image/jpeg",
                "image/png",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        // Validate extension and MIME types
        boolean isValidExtension = allowedExtensions.stream()
                .anyMatch(ext -> fileName.toLowerCase().endsWith(ext));
        String contentType = file.getContentType();
        if (!isValidExtension || !allowedMimeTypes.contains(contentType)) {
            throw new StorageException("file không đúng định dạng. Vui lòng tải lại file");
        }
        // Validate MIME type

        // create folder if not exist
        this.fileServive.createUploadFolder(baseUri + folder);

        // store file
        String uploadFile = this.fileServive.store(file, folder);
        ResUploadFileDTO res = new ResUploadFileDTO(uploadFile, Instant.now());
        return ResponseEntity.ok().body(res);
    }

    @GetMapping("/files")
    @ApiMessage("Download a file")
    public ResponseEntity<Resource> download(@RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "folder", required = false) String folder)
            throws StorageException, URISyntaxException, FileNotFoundException {
        if (fileName == null || folder == null) {
            throw new StorageException("fileName hoac folder khong duoc de trong");
        }
        long fileLength = this.fileServive.getFileLength(fileName, folder);
        if (fileLength == 0) {
            throw new StorageException("File voi ten " + fileName + " khong ton tai");
        }
        InputStreamResource resource = this.fileServive.getResource(fileName, folder);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(fileLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
