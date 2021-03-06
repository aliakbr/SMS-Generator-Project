package smsgenerator.main;

import yusufs.nlp.nerid.IndonesiaNER;
import yusufs.nlp.nerid.utils.TextSequence.*;

import java.io.*;
import java.util.*;

import smsgenerator.module.VerbNounFrequency;

// Asumsi : Subjek adalah kumpulan kata sebelum Verb

public class Main {

    public static ArrayList<String> ReadFile(String path){
        String fileName = path;
        ArrayList<String> output = new ArrayList<String>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

            String line;
            while ((line = br.readLine()) != null) {
                output.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static void main(String[] args) throws FileNotFoundException {
        String path = "./input/input.txt";
        IndonesiaNER iner = new IndonesiaNER(IndonesiaNER.MODEL.YUSUFS);
        ArrayList<String> lines = ReadFile(path);

        ArrayList<VerbNounFrequency> list_of_verb_noun = new ArrayList<VerbNounFrequency>();
        for (String line: lines) {
            String sentence_to_predict = line;
            ArrayList<Sentence> predicted = iner.predictWithEmbeddedModel(sentence_to_predict, true);

            String sentence_subject_to_predict = "";
            String subject = "";
            String verb = "";
            for (Sentence arrWords : predicted) {
                for (Words word : arrWords.getWords()) {
                    String current_word = word.getToken();  // kata yang diprediksi
                    String current_Ner_label = word.getXmlTag(); // hasil prediksi label entitas untuk kata tersebut
                    String current_label_pos = word.getPosTag(); // postag dari kata tersebut
                    // System.out.printf("%s %s %s\n", current_word, current_Ner_label, current_label_pos);

                    // Ambil Verb
                    if (current_label_pos.contains("VB")){
                        verb = current_word;

                        // Analisis Subjek
                        ArrayList<Sentence> subject_predict = iner.predictWithEmbeddedModel(sentence_subject_to_predict, true);
                        for (Sentence arrWords1 : subject_predict){
                            for(Words word1: arrWords1.getWords()){
                                String current_word1 = word1.getToken();
                                String current_Ner_label1 = word.getXmlTag();
                                String current_label_pos1 = word1.getPosTag();

                                // Ambil Noun
                                if (current_label_pos1.contains("NN")){
                                    // Masukkan Subjek dan Verb yang telah dianalisis
                                    Boolean added = false;
                                    for (VerbNounFrequency el: list_of_verb_noun){
                                        if (el.getVerb() == verb){
                                            el.add_noun(current_word1);
                                            added = true;
                                        }
                                    }
                                    if (!added){
                                        list_of_verb_noun.add(new VerbNounFrequency(verb, current_word1));
                                    }
                                }
                            }
                        }
                    }
                    else{
                        current_word = " " + current_word;
                        sentence_subject_to_predict += current_word;
                    }
                }
            }
        }

        // Sort dan print output
        Collections.sort(list_of_verb_noun, new Comparator<VerbNounFrequency>() {
            @Override
            public int compare(VerbNounFrequency o1, VerbNounFrequency o2) {
                return o2.getFreqTotal().compareTo(o1.getFreqTotal());
            }
        });

        System.out.printf("--RESULT----\n");
        for (VerbNounFrequency v : list_of_verb_noun){
            v.print();
        }

        // Write to output
        PrintWriter out = new PrintWriter("./output/output.txt");
        for (VerbNounFrequency v : list_of_verb_noun){
            out.println(v.toString());
        }
    }
};
