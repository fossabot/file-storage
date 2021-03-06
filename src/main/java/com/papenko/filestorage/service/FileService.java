package com.papenko.filestorage.service;

import com.papenko.filestorage.dto.FileValidityCheckReport;
import com.papenko.filestorage.dto.SlimFilePage;
import com.papenko.filestorage.entity.File;
import com.papenko.filestorage.exception.*;
import com.papenko.filestorage.repository.FileCustomRepository;
import com.papenko.filestorage.repository.FileRepository;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FileService {
    private final FileRepository fileRepository;
    private final FileCustomRepository fileCustomRepository;

    public FileService(FileRepository fileRepository, FileCustomRepository fileCustomRepository) {
        this.fileRepository = fileRepository;
        this.fileCustomRepository = fileCustomRepository;
    }

    public File uploadFile(File file) {
        final FileValidityCheckReport report = isFileValid(file);
        if (!report.isValid()) {
            throw new FileUpload400Exception(report.getErrorMessage());
        }
        return fileRepository.save(file);
    }

    FileValidityCheckReport isFileValid(File file) {
        if (Strings.isBlank(file.getName())) {
            return new FileValidityCheckReport(false, "file name is missing");
        }
        if (file.getSize() == null) {
            return new FileValidityCheckReport(false, "file size is missing");
        }
        if (file.getSize() < 0) {
            return new FileValidityCheckReport(false, "file size is negative");
        }
        return new FileValidityCheckReport(true, null);
    }

    public void delete(String id) {
        if (!fileRepository.existsById(id)) {
            throw new FileDelete404Exception();
        }
        fileRepository.deleteById(id);
    }

    public void updateTags(String id, List<String> tags) {
        final Optional<File> fileOptional = fileRepository.findById(id);
        if (fileOptional.isEmpty()) {
            throw new FileUpdateTags404Exception();
        }
        fileRepository.save(fileOptional.get().withTags(tags));
    }
    
    public void deleteTags(String id, List<String> tags) {
        final Optional<File> fileOptional = fileRepository.findById(id);
        if (fileOptional.isEmpty()) {
            throw new FileDeleteTags404Exception();
        }
        final File file = fileOptional.get();
        if (!file.getTags().containsAll(tags)) {
            throw new FileDeleteTags400Exception();
        }
        final File withTags = file.withTags(file.getTags().stream()
                .filter(tag -> !tags.contains(tag))
                .collect(Collectors.toList()));
        fileRepository.save(withTags);
    }

    public SlimFilePage findPageByTagsAndName(List<String> tags, Pageable pageable, String name) {
        Page<File> found = fileCustomRepository.findAllByTagsContainingAllIn(tags, pageable, name);
        return new SlimFilePage(found.getTotalElements(), found.getContent());
    }
}
