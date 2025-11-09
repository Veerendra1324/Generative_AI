public class Student {
    private String name;
    private String[] subjects;
    private double[] marks;

    public Student(String name, String[] subjects, double[] marks) {
        this.name = name;
        this.subjects = subjects;
        this.marks = marks;
    }

    public String getName() { return name; }
    public String[] getSubjects() { return subjects; }
    public double[] getMarks() { return marks; }

    public double total() {
        double s = 0;
        if (marks != null) {
            for (double m : marks) s += m;
        }
        return s;
    }

    public double average() {
        if (marks == null || marks.length == 0) return 0.0;
        return total() / marks.length;
    }
}
