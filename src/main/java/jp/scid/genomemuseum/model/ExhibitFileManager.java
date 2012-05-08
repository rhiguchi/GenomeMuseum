package jp.scid.genomemuseum.model;

import static java.lang.String.*;

import java.io.File;
import java.io.IOException;

import jp.scid.genomemuseum.model.MuseumExhibit.FileType;

import org.apache.commons.io.FileUtils;

public class ExhibitFileManager {
    private File directory;
    
    private String genbankExtension = "gbk";
    private String fastaExtension = "fasta";
    private String otherFileExtension = "txt";

    public File getDirectory() {
        return directory;
    }
    
    public void setDirectory(File directory) {
        this.directory = directory;
    }
    
    public File storeFileToLibrary(MuseumExhibit exhibit, File sourceFile) throws IOException {
        String extension = getExtension(exhibit.getFileType());
        String baseName =
            getBaseName(exhibit.getName(), exhibit.getAccession(), exhibit.getDefinition());

        File outFile = new File(directory, baseName + extension);
        int baseCount = 1;
        
        while (!outFile.createNewFile()) {
            if (!directory.canWrite())
                throw new IllegalStateException(format("writing to dir %s is not allowed", directory));
            
            outFile = new File(directory, baseName + " " + baseCount++ + extension);
        }
        
        FileUtils.copyFile(sourceFile, outFile, true);
        
        return outFile;
    }

    String getBaseName(String name, String accession, String definition) {
        final String baseName;
        
        if (name != null && !name.isEmpty()) {
            baseName = name;
        }
        else if (accession != null && !accession.isEmpty()) {
            baseName = accession;
        }
        else if (definition != null && !definition.isEmpty()) {
            baseName = definition;
        }
        else {
            baseName = "Untitled";
        }
        return baseName;
    }

    String getExtension(FileType fileType) {
        final String extension;
        
        if (fileType == FileType.GENBANK) {
            extension = genbankExtension;
        }
        else if (fileType == FileType.FASTA) {
            extension = fastaExtension;
        }
        else {
            extension = otherFileExtension;
        }
        return extension;
    }
}