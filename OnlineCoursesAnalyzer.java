import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * This is just a demo for you, please run it on JDK17 (some statements may be not allowed in lower version).
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.
 */
public class OnlineCoursesAnalyzer {
    static List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                        Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                        Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                        Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1
    public Map<String, Integer> getPtcpCountByInst() {
        Map<String, Integer> map=courses
                .stream()
                .collect(Collectors
                        .groupingBy(Course::getInstitution,Collectors.summingInt(Course::getParticipants)));
        Map<String, Integer> sortedMap=new TreeMap<>(map);
        return sortedMap;
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String, Integer> map=courses
                .stream()
                .collect(Collectors.groupingBy(course->course.getInstitution()+"-"+course.getSubject(),Collectors.summingInt(Course::getParticipants)));
        Map<String, Integer> sortedMap=new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int result=map.get(o2).compareTo(map.get(o1));
                if (result==0)
                    return o1.compareTo(o2);
                else return result;
            }
        });
        sortedMap.putAll(map);
        List<String> string=new ArrayList<>();
        return sortedMap;
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        Map<String, List<List<String>>> map=new HashMap<>();
        List<String> instructors= courses.stream().map(Course::getInstructors).flatMap(List::stream).distinct().toList();
        for (String string:instructors){
            if (string.contains("Note"))
                string=string.substring(4);
            List<List<String>> course=new ArrayList<>();
            List<String> list1=new ArrayList<>();
            List<String> list2=new ArrayList<>();
            course.add(new ArrayList<>());
            course.add(new ArrayList<>());
            map.put(string,course);
        }
        courses.stream().forEach(course -> {
                    course.getInstructors().stream().forEach(
                            instructor->{
                                if (instructor.contains("Note")) {
                                    instructor=instructor.substring(4);
                                    List<String> The_next=map.get(instructor).get(1);
                                    The_next.add(course.getTitle());
                                    List<List<String>> the_next=map.get(instructor);
                                    the_next.set(1,The_next);
                                    map.replace(instructor, the_next);
                                }
                                else {
                                    List<String> The_next=map.get(instructor).get(0);
                                    The_next.add(course.getTitle());
                                    List<List<String>> the_next=map.get(instructor);
                                    the_next.set(0,The_next);
                                    map.replace(instructor, the_next);
                                }
                            }
                    );
                });
        for (String string:map.keySet()){
            List<String> list1=map.get(string).get(0).stream()
                    .distinct()
                    .sorted()
                    .toList();
            List<String> list2=map.get(string).get(1).stream()
                    .distinct()
                    .sorted()
                    .toList();
            List<List<String>> The_new=new ArrayList<>();
            The_new.add(list1);
            The_new.add(list2);
            map.replace(string, The_new);
        }
        return map;
    }

    //4
    public List<String> getCourses(int topK, String by) {
        List<String> titles=new ArrayList<>();
        if(Objects.equals(by,"hours")){
            titles=courses.stream()
                    .sorted(Comparator.comparing((Course::getTotalHours),Comparator.reverseOrder()).thenComparing(Course::getTitle))
                    .map(Course::getTitle)
                    .distinct()
                    .limit(topK)
                    .toList();
        }
        else {
            titles=courses.stream()
                    .sorted(Comparator.comparing((Course::getParticipants),Comparator.reverseOrder()).thenComparing(Course::getTitle))
                    .map(Course::getTitle)
                    .distinct()
                    .limit(topK)
                    .toList();
        }
        return titles;
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        String courseSubjectL=courseSubject.toLowerCase();
        List<String> list= courses.stream()
                .filter(course -> {
                    for (String subject : course.getSubjects()) {
                        String subjectL = subject.toLowerCase();
                        if (fuzzyMatch(subjectL, courseSubjectL)) {
                            return true;
                        }
                    }
                    return false;
                })
                .filter(course -> course.getPercentAudited() >= percentAudited)
                .filter(course -> course.getTotalHours() <= totalCourseHours)
                .map(Course::getTitle)
                .distinct()
                .sorted(String::compareTo)
                .toList();
        return list;
    }
    public boolean fuzzyMatch(String input, String goal){
        if(input==null||goal==null)
            return false;
        return input.equalsIgnoreCase(goal);
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) throws ParseException {
        Map<String, Date> map=new HashMap<>();
        List<String> Numbers= courses.stream().map(Course::getNumber).distinct().toList();
        for (String string:Numbers){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date=sdf.parse("2000-01-01");
            map.put(string,date);
        }
        Map<String, Double> Age=courses.stream().collect(Collectors.groupingBy(Course::getNumber,Collectors.averagingDouble(Course::getMedianAge)));
        Map<String, Double> Gender=courses.stream().collect(Collectors.groupingBy(Course::getNumber,Collectors.averagingDouble(Course::getPercentMale)));
        Map<String, Double> Bachelor=courses.stream().collect(Collectors.groupingBy(Course::getNumber,Collectors.averagingDouble(Course::getPercentDegree)));
        courses.stream().forEach(course -> {
            if (map.get(course.getNumber()).compareTo(course.getLaunchDate())<0)
                map.replace(course.getNumber(),course.getLaunchDate());
        });
        List<String> Top10= courses.stream()
                .filter(course -> {
                    if (map.get(course.getNumber()).compareTo(course.getLaunchDate())==0) {
                        return true;
                    }
                    return false;
                })
                .map(course -> {
                    double similarity = Math.pow(age - Age.get(course.getNumber()), 2) + Math.pow(100 * gender - Gender.get(course.getNumber()), 2) + Math.pow(100 * isBachelorOrHigher - Bachelor.get(course.getNumber()), 2);
                    return new Similarity(similarity, course.getTitle());
                })
                .sorted(Comparator.comparing(Similarity::getSimilarity).thenComparing(Similarity::getTitle))
                .map(Similarity::getTitle)
                .distinct()
                .limit(10).toList();
        return Top10;
    }
}

class Course {
    String institution;
    String number;
    Date launchDate;
    String title;
    List<String> instructors;
    String subject;
    List<String> subjects;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;
    double similarity;

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public String getInstitution() {
        return institution;
    }

    public String getNumber() {
        return number;
    }

    public Date getLaunchDate() {
        return launchDate;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getInstructors() {
        return instructors;
    }

    public String getSubject() {
        return subject;
    }

    public int getYear() {
        return year;
    }

    public int getHonorCode() {
        return honorCode;
    }

    public int getParticipants() {
        return participants;
    }

    public int getAudited() {
        return audited;
    }

    public int getCertified() {
        return certified;
    }

    public double getPercentAudited() {
        return percentAudited;
    }

    public double getPercentCertified() {
        return percentCertified;
    }

    public double getPercentCertified50() {
        return percentCertified50;
    }

    public double getPercentVideo() {
        return percentVideo;
    }

    public double getPercentForum() {
        return percentForum;
    }

    public double getGradeHigherZero() {
        return gradeHigherZero;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public double getMedianHoursCertification() {
        return medianHoursCertification;
    }

    public double getMedianAge() {
        return medianAge;
    }

    public double getPercentMale() {
        return percentMale;
    }

    public double getPercentFemale() {
        return percentFemale;
    }

    public double getPercentDegree() {
        return percentDegree;
    }

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) title = title.substring(1);
        if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
        this.title = title;
        if (instructors.startsWith("\"")) instructors = instructors.substring(1);
        if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
        if (instructors.contains(", "))
            this.instructors = Arrays.stream(instructors.split(", "))
                    .filter(element->!Objects.equals(element,""))
                    .map(element->"Note"+element)
                    .collect(Collectors.toList());
        else this.instructors = Arrays.stream(instructors.split(", "))
                .filter(element->!Objects.equals(element,""))
                .collect(Collectors.toList());
        if (subject.startsWith("\"")) subject = subject.substring(1);
        if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
        this.subject =subject;
        this.subjects=Arrays.stream(subject.split((", |and | ")))
                .filter(element->!Objects.equals(element,""))
                .collect(Collectors.toList());
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }
}
class Subject_participants{
    private String subject;
    private int participant;
    private String institution;

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public Subject_participants(String institution, String subject, int participant){
        this.institution=institution;
        this.participant=participant;
        this.subject=subject;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getParticipant() {
        return participant;
    }

    public void setParticipant(int participant) {
        this.participant = participant;
    }
}
class Similarity{
    private double similarity;
    private String title;
    public Similarity(double similarity, String title){
        this.similarity=similarity;
        this.title=title;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(int similarity) {
        this.similarity = similarity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}