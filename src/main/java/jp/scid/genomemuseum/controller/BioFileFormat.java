package jp.scid.genomemuseum.controller;

import java.io.Reader;
import java.text.ParseException;

import jp.scid.bio.Fasta;
import jp.scid.bio.FastaParser;
import jp.scid.bio.GenBank;
import jp.scid.bio.GenBankParser;

public enum BioFileFormat {
    GEN_BANK {
        @Override
        protected String getSequence(Reader reader) {
            GenBankParser parser = new GenBankParser();
            String sequence = "";
            try {
                GenBank data = parser.parseFrom(reader);
                sequence = data.origin().sequence();
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
            return sequence;
        }
    },
    FASTA {
        @Override
        protected String getSequence(Reader reader) {
            FastaParser parser = new FastaParser();
            String sequence = "";
            try {
                Fasta data = parser.parseFrom(reader);
                sequence = data.sequence().value();
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
            return sequence;
        }
    };
    
    abstract protected String getSequence(Reader reader); 
}