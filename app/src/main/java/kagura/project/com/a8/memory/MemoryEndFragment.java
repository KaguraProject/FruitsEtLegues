package kagura.project.com.a8.memory;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import kagura.project.com.a8.R;


public class MemoryEndFragment extends Fragment {

    int level;
    Button buttonLevel;

    public MemoryEndFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        level = this.getArguments().getInt("level");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        ConstraintLayout rl = (ConstraintLayout) inflater.inflate(R.layout.fragment_memory_end, container, false);
        if(level == 3){
            buttonLevel = (Button) rl.findViewById(R.id.buttonNextLevel);
            buttonLevel.setEnabled(false);
        }

        return rl;

    }
}
