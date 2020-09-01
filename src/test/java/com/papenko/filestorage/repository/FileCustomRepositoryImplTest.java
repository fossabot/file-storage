package com.papenko.filestorage.repository;

import com.papenko.filestorage.entity.File;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileCustomRepositoryImplTest {
    @InjectMocks
    @Spy
    FileCustomRepositoryImpl fileCustomRepository;
    @Mock
    ElasticsearchOperations operations;

    @Test
    void getQueryBuilder_shouldCreateEmptyNativeQueryBuilder_whenTagsListIsNull() {
        final NativeSearchQuery query = fileCustomRepository.getQueryBuilder(null);

        assertNull(query.getFilter());
    }

    @Test
    void getQueryBuilder_shouldCreatePoperyNativeQueryBuilder_whenTagsListIsNotNull() {
        final NativeSearchQuery query = fileCustomRepository.getQueryBuilder(List.of("tag1", "tag2"));

        assertEquals(QueryBuilders.regexpQuery("tags", "(?=tag1)(?=tag2)"), query.getFilter());
    }

    @Test
    void findAllByTagsContainingAllIn_shouldCallConvertToPage_whenSingleDocumentIsFound() {
        var file = new File("id", "name", 0L, List.of("tag1", "tag2", "tag3"));
        var searchHit = new SearchHit<>("id", 1, null, null, file);
        var searchHits = new SearchHitsImpl<>(1, TotalHitsRelation.EQUAL_TO, 1, null, List.of(searchHit), null);
        doReturn(searchHits).when(operations).search((Query) any(), any()); //regexp query: (?=.+tag1)(?=.+tag2).+

        Page<File> actual = fileCustomRepository
                .findAllByTagsContainingAllIn(List.of("tag1", "tag2"), PageRequest.of(0, 10));

        verify(fileCustomRepository).convertToPage(searchHits, PageRequest.of(0, 10));
        Page<File> expected = fileCustomRepository.convertToPage(searchHits, PageRequest.of(0, 10));
        assertEquals(expected, actual);
    }

    @Test
    void convertToPage_shouldCovertToPage_whenSuppliedSearchHitsAndPageable() {
        var file = new File("id", "name", 0L, List.of("tag1", "tag2", "tag3"));
        var searchHit = new SearchHit<>("id", 1, null, null, file);
        var searchHits = new SearchHitsImpl<>(1, TotalHitsRelation.EQUAL_TO, 1, null, List.of(searchHit), null);

        final Page<File> files = fileCustomRepository.convertToPage(searchHits, PageRequest.of(0, 10));

        assertEquals(1, files.getTotalElements());
        assertEquals(1, files.getTotalPages());
        assertEquals(file, files.get().findFirst().orElse(null));
    }
}
