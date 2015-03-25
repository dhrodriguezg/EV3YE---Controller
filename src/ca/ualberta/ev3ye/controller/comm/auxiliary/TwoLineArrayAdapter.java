package ca.ualberta.ev3ye.controller.comm.auxiliary;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import ca.ualberta.ev3ye.controller.R;

/**
 * Created by Yuey on 2015-03-18.
 */
public class TwoLineArrayAdapter
		extends ArrayAdapter< Pair< String, String > >
{
	protected List< Pair< String, String > > data = null;

	public TwoLineArrayAdapter( Context context, List< Pair< String, String > > objects )
	{
		super( context, R.layout.list_item_spinner, objects );
		data = objects;
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent )
	{
		ViewHolder viewHolder;

		if ( convertView == null )
		{
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			convertView = inflater.inflate( R.layout.list_item_spinner, parent, false );
			viewHolder = new ViewHolder( convertView );
			convertView.setTag( viewHolder );
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		Pair<String, String> item = data.get( position );

		viewHolder.title.setText( item.first );
		viewHolder.subtitle.setText( item.second );

		return convertView;
	}

	@Override
	public View getDropDownView( int position, View convertView, ViewGroup parent )
	{
		return getView( position, convertView, parent );
	}

	protected class ViewHolder
	{
		public TextView title;
		public TextView subtitle;

		public ViewHolder( View v )
		{
			title = (TextView) v.findViewById( R.id.li_spinner_title );
			subtitle = (TextView) v.findViewById( R.id.li_spinner_subtitle );
		}
	}
}
