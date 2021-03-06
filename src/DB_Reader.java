/**
 * Created by Hugo Lucas on 10/28/2016.
 */
import java.sql.*;
import java.util.ArrayList;

public class DB_Reader implements Database_Reader{

    private Connection reader_connection;   //Always use connection in shortest possible scope
    static private String db_username;
    static private String db_password;
    static private String url;

    public DB_Reader(){
        this.db_username = "root";
        this.db_password = "mysql";
        this.url = "jdbc:mysql://localhost:3306/time_management_system";
    }

    /**
     * Given a user's login information, method will return the employee_number of the
     * user if the login credentials are valid. Invalid credentials will result in negative
     * value.
     * @param input_password    password retrieved by GUI page
     * @param input_username    username retrieved by GUI page
     * @return  Employee number of user if login is valid, -1 if invalid, -2 if other errors
     */
    public int login_user(String input_password, String input_username) {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            Connection reader_connection = DriverManager.getConnection(url, db_username, db_password);

            PreparedStatement stmt = reader_connection.prepareStatement("select * from login_system where user_name = ?");
            stmt.setString(1, input_username);

            ResultSet queryResult = stmt.executeQuery();

            /* Check if statement contains rows with matching db_username */
            while (queryResult.next())
                if (queryResult.getString("password").equals(input_password))
                    return queryResult.getInt("employee_number");

            reader_connection.close();
            return -1;

        } catch (ClassNotFoundException e) {
            /* JAR may not be configured right or JDBC may not be working */
            e.printStackTrace();
        } catch (SQLException e) {
            /* Catch all for errors I have not yet encountered */
            e.printStackTrace();
        } finally {
            if (reader_connection != null)
                try { reader_connection.close(); }catch (Exception e){ /* Ignore this I guess! */}
            return -2;
        }
    }

    /**
     * Given a valid employee identification number, method will return a list of
     * projects an employee can work on.
     *
     * @param employee_number EmployeeID
     * @return list of all projects an employee can work, can be used by GUI to populate
     * input selection menus
     */
    @Override
    public ArrayList<EmployeeProject> projectsAvailable(int employee_number) {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            Connection reader_connection = DriverManager.getConnection(url, db_username, db_password);
            String query = "SELECT TaskID FROM employee_task_map WHERE EmployeeID = ?";
            PreparedStatement stmt = reader_connection.prepareStatement(query);
            stmt.setInt(1, employee_number);

            ResultSet queryResult = stmt.executeQuery();
            ArrayList<Integer> projs = new ArrayList<Integer>();
            while (queryResult.next()){
                int taskID = queryResult.getInt("TaskID");

                query = "SELECT ProjectID FROM project_task_map WHERE TaskID = ?";
                PreparedStatement stmt_inner = reader_connection.prepareStatement(query);
                stmt_inner.setInt(1, taskID);

                ResultSet queryResult_inner = stmt_inner.executeQuery();

                while(queryResult_inner.next()) {
                    int projectID = queryResult_inner.getInt("ProjectID");
                    if (!projs.contains(projectID))
                        projs.add(projectID);
                }

                stmt_inner.close();
                queryResult_inner.close();
            }

            stmt.close();
            queryResult.close();

            if (projs.size() > 0) {
                ArrayList<EmployeeProject> projectList = new ArrayList<EmployeeProject>(projs.size());
                for (int projectID: projs){
                    query = "SELECT ProjectName FROM projects WHERE ProjectID = ?";
                    stmt = reader_connection.prepareStatement(query);
                    stmt.setInt(1, projectID);

                    queryResult = stmt.executeQuery();

                    while(queryResult.next())
                        projectList.add(new EmployeeProject(queryResult.getString("ProjectName"), projectID));
                }

                return projectList;
            } else {return null; }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader_connection != null)
                try {
                    reader_connection.close();
                } catch (Exception e) { /* Ignore this I guess! */}
        }
    }


    /**
     * Given a valid employee identification number, method will return a list of
     * tasks an employee can work on.
     *
     * @param employee_number EmployeeID
     * @return list of all tasks an employee can work, can be used by GUI to populate
     * input selection menus
     */
    @Override
    public ArrayList<EmployeeTask> tasksAvailable(int employee_number) {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            Connection reader_connection = DriverManager.getConnection(url, db_username, db_password);
            String query = "SELECT TaskID FROM employee_task_map WHERE EmployeeID = ?";
            PreparedStatement stmt = reader_connection.prepareStatement(query);
            stmt.setInt(1, employee_number);

            ResultSet queryResult = stmt.executeQuery();
            ArrayList<EmployeeTask> tasks = new ArrayList<EmployeeTask>();
            while(queryResult.next()){
                int taskID = queryResult.getInt("TaskID");
                if(!tasks.contains(taskID)){
                    query = "SELECT TaskName FROM tasks WHERE TaskID = ?";
                    stmt = reader_connection.prepareStatement(query);
                    stmt.setInt(1, taskID);

                    ResultSet queryResult_inner = stmt.executeQuery();
                    while (queryResult_inner.next()){
                        String taskName = queryResult_inner.getString("TaskName");
                        tasks.add(new EmployeeTask(taskName, taskID));
                    }
                    queryResult_inner.close();
                }
            }

            stmt.close();
            queryResult.close();

            return tasks;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader_connection != null)
                try {
                    reader_connection.close();
                } catch (Exception e) { /* Ignore this I guess! */}
        }
    }

    /**
     * Helper method for employee times sheet generation.
     *
     * @param list list of all employee whose work hours are needed
     * @return list of all employee's work hours
     */
    @Override
    public ArrayList<EmployeeLog> employeeWorkHours(ArrayList<Employee> list){
        ArrayList<EmployeeLog> timeLogs = new ArrayList<>();
        try {
            for (Employee currentEmployee: list) {
                Class.forName("com.mysql.jdbc.Driver");

                Connection reader_connection = DriverManager.getConnection(url, db_username, db_password);
                String query = "SELECT TimeIn, TimeOut, Date, TaskID FROM time_logs WHERE EmployeeID = ?";
                PreparedStatement stmt = reader_connection.prepareStatement(query);
                stmt.setInt(1, currentEmployee.getEmployeeNumber());

                ResultSet queryResult = stmt.executeQuery();

                while(queryResult.next())
                    timeLogs.add(new EmployeeLog(queryResult.getTime("TimeIn"),
                            queryResult.getTime("TimeOut"), queryResult.getDate("Date"),
                            queryResult.getInt("TaskID")));

                stmt.close();
                queryResult.close();
            }

            return timeLogs;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader_connection != null)
                try {
                    reader_connection.close();
                } catch (Exception e) { /* Ignore this I guess! */}
        }
    }

    /**
     * Generates a list of all employees currently working at the company. Used
     * by GUI in manager report generation.
     *
     * @return list of employees
     */
    @Override
    public ArrayList<Employee> allEmployees() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection reader_connection = DriverManager.getConnection(url, db_username, db_password);
            String query = "SELECT EmployeeID, FirstName, LastName, EndDate FROM employee";
            PreparedStatement stmt = reader_connection.prepareStatement(query);
            ResultSet queryResult = stmt.executeQuery();

            ArrayList<Employee> empList = new ArrayList<Employee>();
            while(queryResult.next()){
                String endDate = queryResult.getString("EndDate");

                if(endDate == null){
                    int employeeId = queryResult.getInt("EmployeeID");
                    String firstName = queryResult.getString("FirstName");
                    String lastName = queryResult.getString("LastName");

                    empList.add(new Employee(firstName, lastName, employeeId));
                }
            }
            stmt.close();
            queryResult.close();

            return empList;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader_connection != null)
                try {
                    reader_connection.close();
                } catch (Exception e) { /* Ignore this I guess! */}
        }
    }


}
